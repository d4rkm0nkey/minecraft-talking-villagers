import { audioContext } from "../audio/browser";
import { toDb } from "../audio/dsp";
import type { Audio } from "../audio/process";

/**
 * Records the microphone as raw PCM. Browser processing (noise suppression, echo cancellation, auto gain) is switched
 * off: it differs between browsers and colours the voice. Noise reduction happens afterwards and can be undone.
 */

export interface Level {
  peakDb: number;
  rmsDb: number;
}

/** How several input channels become mono: averaged, or one of them (0-based). */
export type InputChannel = "mix" | number;

/** The level is shown at most this often; the audio thread reports it about 375 times a second. */
const LEVEL_INTERVAL_MS = 50;

export async function microphones(): Promise<MediaDeviceInfo[]> {
  const devices = await navigator.mediaDevices.enumerateDevices();
  return devices.filter((device) => device.kind === "audioinput");
}

export class Recorder {
  private stream?: MediaStream;
  private node?: AudioWorkletNode;
  private source?: MediaStreamAudioSourceNode;
  private chunks: Float32Array[] = [];
  private samples = 0;
  private recording = false;
  /** Increased by every open and close, so an open that finishes after close() gives its microphone back. */
  private generation = 0;
  private channel: InputChannel = "mix";
  private levelPeak = 0;
  private levelRms = 0;
  private lastLevel = 0;
  /** Input channels of the microphone, known after the first audio arrived. */
  inputChannels = 1;
  onLevel?: (level: Level) => void;
  /** Called for every block of audio with its peak, e.g. to detect clipping, which a throttled level would miss. */
  onPeak?: (peak: number) => void;

  async open(deviceId?: string): Promise<void> {
    this.close();
    const generation = ++this.generation;
    const ctx = audioContext();
    await ctx.resume();
    await ctx.audioWorklet.addModule(new URL("recorder-worklet.js", document.baseURI).href);
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        deviceId: deviceId ? { exact: deviceId } : undefined,
        echoCancellation: false,
        noiseSuppression: false,
        autoGainControl: false,
      },
    });
    if (generation !== this.generation) {
      // closed while the browser was asking for permission, e.g. the studio was left
      stream.getTracks().forEach((track) => track.stop());
      return;
    }
    this.stream = stream;
    this.source = ctx.createMediaStreamSource(stream);
    this.node = new AudioWorkletNode(ctx, "recorder");
    this.node.port.postMessage({ type: "channel", channel: this.channel });
    this.node.port.onmessage = (event: MessageEvent<{ samples: Float32Array; peak: number; rms: number; channels: number }>) => {
      const { samples, peak, rms, channels } = event.data;
      this.inputChannels = channels;
      if (this.recording) {
        this.chunks.push(samples);
        this.samples += samples.length;
      }
      this.onPeak?.(peak);
      this.levelPeak = Math.max(this.levelPeak, peak);
      this.levelRms = Math.max(this.levelRms, rms);
      const now = performance.now();
      if (now - this.lastLevel >= LEVEL_INTERVAL_MS) {
        this.onLevel?.({ peakDb: toDb(this.levelPeak), rmsDb: toDb(this.levelRms) });
        this.levelPeak = 0;
        this.levelRms = 0;
        this.lastLevel = now;
      }
    };
    this.source.connect(this.node);
  }

  get isOpen(): boolean {
    return this.stream !== undefined;
  }

  /** Which input channel is recorded when the microphone has more than one. */
  setChannel(channel: InputChannel): void {
    this.channel = channel;
    this.node?.port.postMessage({ type: "channel", channel });
  }

  start(): void {
    this.chunks = [];
    this.samples = 0;
    this.recording = true;
  }

  /** Length of the running recording in seconds, without copying it. */
  seconds(): number {
    return this.samples / audioContext().sampleRate;
  }

  /** Samples recorded so far, e.g. to detect the end of speech while recording. */
  current(): Audio {
    return { sampleRate: audioContext().sampleRate, data: join(this.chunks) };
  }

  stop(): Audio {
    this.recording = false;
    return this.current();
  }

  close(): void {
    this.generation++;
    this.recording = false;
    this.source?.disconnect();
    if (this.node) {
      this.node.port.postMessage({ type: "stop" });
      this.node.port.onmessage = null;
      this.node.disconnect();
    }
    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = undefined;
    this.source = undefined;
    this.node = undefined;
  }
}

function join(chunks: Float32Array[]): Float32Array {
  const out = new Float32Array(chunks.reduce((sum, chunk) => sum + chunk.length, 0));
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

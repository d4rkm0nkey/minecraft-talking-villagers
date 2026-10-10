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

export async function microphones(): Promise<MediaDeviceInfo[]> {
  const devices = await navigator.mediaDevices.enumerateDevices();
  return devices.filter((device) => device.kind === "audioinput");
}

export class Recorder {
  private stream?: MediaStream;
  private node?: AudioWorkletNode;
  private source?: MediaStreamAudioSourceNode;
  private chunks: Float32Array[] = [];
  private recording = false;
  onLevel?: (level: Level) => void;

  async open(deviceId?: string): Promise<void> {
    this.close();
    const ctx = audioContext();
    await ctx.resume();
    await ctx.audioWorklet.addModule(new URL("recorder-worklet.js", document.baseURI).href);
    this.stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        deviceId: deviceId ? { exact: deviceId } : undefined,
        channelCount: 1,
        echoCancellation: false,
        noiseSuppression: false,
        autoGainControl: false,
      },
    });
    this.source = ctx.createMediaStreamSource(this.stream);
    this.node = new AudioWorkletNode(ctx, "recorder");
    this.node.port.onmessage = (event: MessageEvent<{ samples: Float32Array; peak: number; rms: number }>) => {
      if (this.recording) this.chunks.push(event.data.samples);
      this.onLevel?.({ peakDb: toDb(event.data.peak), rmsDb: toDb(event.data.rms) });
    };
    this.source.connect(this.node);
  }

  get isOpen(): boolean {
    return this.stream !== undefined;
  }

  start(): void {
    this.chunks = [];
    this.recording = true;
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
    this.recording = false;
    this.source?.disconnect();
    this.node?.disconnect();
    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = undefined;
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

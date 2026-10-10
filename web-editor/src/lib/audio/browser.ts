import type { DenoiseState, Rnnoise } from "@shiguredo/rnnoise-wasm";
import { resample, toMono } from "./dsp";
import type { Audio } from "./process";

/**
 * Audio functions that need the browser: decoding files, playback and neural noise suppression.
 */

let context: AudioContext | undefined;
export function audioContext(): AudioContext {
  context ??= new AudioContext();
  return context;
}

/** Decodes any format the browser understands (WAV, MP3, OGG, FLAC, M4A …) to mono PCM. */
export async function decodeFile(data: ArrayBuffer): Promise<Audio> {
  const buffer = await audioContext().decodeAudioData(data.slice(0));
  const channels = Array.from({ length: buffer.numberOfChannels }, (_, i) => buffer.getChannelData(i));
  return { sampleRate: buffer.sampleRate, data: toMono(channels) };
}

let playing: AudioBufferSourceNode | undefined;

/** Plays audio; a higher rate also raises the pitch, like Minecraft does. */
export function play(audio: Audio, rate = 1, onEnded?: () => void): void {
  stop();
  const ctx = audioContext();
  void ctx.resume();
  const buffer = ctx.createBuffer(1, audio.data.length, audio.sampleRate);
  buffer.copyToChannel(audio.data as Float32Array<ArrayBuffer>, 0);
  const source = ctx.createBufferSource();
  source.buffer = buffer;
  source.playbackRate.value = rate;
  source.connect(ctx.destination);
  source.onended = () => {
    if (playing === source) playing = undefined;
    onEnded?.();
  };
  source.start();
  playing = source;
}

export function stop(): void {
  playing?.stop();
  playing = undefined;
}

let rnnoise: Promise<Rnnoise> | undefined;

/**
 * RNNoise works on 48 kHz audio in frames of 480 samples scaled like 16 bit PCM.
 */
export async function denoise(pcm: Float32Array, sampleRate: number): Promise<Float32Array> {
  // the model is several megabytes, so it is only loaded when noise suppression is used
  rnnoise ??= import("@shiguredo/rnnoise-wasm").then((module) => module.Rnnoise.load());
  const instance = await rnnoise;
  const state: DenoiseState = instance.createDenoiseState();
  try {
    const input = resample(pcm, sampleRate, 48000);
    const out = new Float32Array(input.length);
    const frame = new Float32Array(instance.frameSize);
    for (let start = 0; start < input.length; start += frame.length) {
      frame.fill(0);
      for (let i = 0; i < frame.length && start + i < input.length; i++) frame[i] = input[start + i] * 32768;
      state.processFrame(frame);
      for (let i = 0; i < frame.length && start + i < out.length; i++) out[start + i] = frame[i] / 32768;
    }
    return resample(out, 48000, sampleRate);
  } finally {
    state.destroy();
  }
}

/** Offers bytes as a file download. */
export function download(bytes: Uint8Array, name: string, type = "application/zip"): void {
  const url = URL.createObjectURL(new Blob([bytes as Uint8Array<ArrayBuffer>], { type }));
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

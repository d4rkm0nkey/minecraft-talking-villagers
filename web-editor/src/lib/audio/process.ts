import type { Line, Processing } from "../model/types";
import { compress, highPass, noiseGate, normalize, resample, speechThreshold, frameLevels, trimSilence } from "./dsp";
import { spectralGate } from "./spectralGate";

export interface Audio {
  sampleRate: number;
  data: Float32Array;
  /** The original Ogg Vorbis file, for lines opened from an existing pack; exported unchanged if nothing was edited. */
  ogg?: Uint8Array;
}

/** Optional neural denoiser, only available in the browser. */
export type Denoiser = (pcm: Float32Array, sampleRate: number) => Promise<Float32Array>;

/**
 * Turns a raw recording into the audio that is exported: manual trim, high-pass, noise reduction, automatic trim,
 * noise gate, compression, normalisation and sample rate conversion. The raw recording is never changed.
 */
export async function processLine(raw: Audio, line: Pick<Line, "trimStart" | "trimEnd">, settings: Processing,
                                  noiseProfile: Float32Array | undefined, denoise: Denoiser | undefined): Promise<Audio> {
  const { sampleRate } = raw;
  const start = Math.min(raw.data.length, Math.round(line.trimStart * sampleRate));
  const end = Math.max(start, raw.data.length - Math.round(line.trimEnd * sampleRate));
  let pcm: Float32Array = raw.data.slice(start, end);

  if (settings.highPass) pcm = highPass(pcm, sampleRate);
  if (settings.denoise && denoise) pcm = await denoise(pcm, sampleRate);
  if (settings.spectralGate && noiseProfile) pcm = spectralGate(pcm, noiseProfile, settings.spectralStrength);
  if (settings.trim) {
    const speech = trimSilence(pcm, sampleRate);
    pcm = pcm.slice(speech.start, speech.end);
  }
  if (settings.noiseGate) pcm = noiseGate(pcm, sampleRate, speechThreshold(frameLevels(pcm, sampleRate)) - 6);
  if (settings.compress) pcm = compress(pcm, sampleRate);
  if (settings.normalize) pcm = normalize(pcm, -1);
  return { sampleRate: settings.sampleRate, data: resample(pcm, sampleRate, settings.sampleRate) };
}

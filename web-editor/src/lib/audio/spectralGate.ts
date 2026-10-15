import { fft } from "./fft";

/**
 * Noise reduction with a noise profile: the spectrum of a few seconds of room tone is measured, and frequencies
 * that are not clearly louder than that in the recording are turned down. Works well against steady noise like
 * fans or hum.
 */

const SIZE = 1024;
const HOP = SIZE / 4;
const WINDOW = Float32Array.from({ length: SIZE }, (_, i) => 0.5 - 0.5 * Math.cos((2 * Math.PI * i) / SIZE));

/** Average magnitude per frequency bin of a recording of silence. */
export function noiseProfile(noise: Float32Array): Float32Array {
  const profile = new Float32Array(SIZE / 2 + 1);
  const re = new Float32Array(SIZE);
  const im = new Float32Array(SIZE);
  let frames = 0;
  for (let start = 0; start + SIZE <= noise.length; start += HOP) {
    for (let i = 0; i < SIZE; i++) {
      re[i] = noise[start + i] * WINDOW[i];
      im[i] = 0;
    }
    fft(re, im);
    for (let k = 0; k < profile.length; k++) profile[k] += Math.hypot(re[k], im[k]);
    frames++;
  }
  return frames === 0 ? profile : profile.map((value) => value / frames);
}

/**
 * @param strength 0 (off) to 1 (maximal reduction)
 */
export function spectralGate(pcm: Float32Array, profile: Float32Array, strength = 0.8): Float32Array {
  if (strength <= 0 || pcm.length < SIZE) return pcm.slice();
  const out = new Float32Array(pcm.length + SIZE);
  const norm = new Float32Array(pcm.length + SIZE);
  const re = new Float32Array(SIZE);
  const im = new Float32Array(SIZE);
  const bins = SIZE / 2 + 1;
  const threshold = 1.5 + strength * 1.5;
  const floor = 1 - strength * 0.95;
  let previous = new Float32Array(bins).fill(1);

  for (let start = 0; start < pcm.length; start += HOP) {
    for (let i = 0; i < SIZE; i++) {
      re[i] = (pcm[start + i] ?? 0) * WINDOW[i];
      im[i] = 0;
    }
    fft(re, im);
    const gains = new Float32Array(bins);
    for (let k = 0; k < bins; k++) {
      const magnitude = Math.hypot(re[k], im[k]);
      const target = magnitude > profile[k] * threshold ? 1 : floor;
      // smooth over time against "musical noise"
      gains[k] = previous[k] * 0.5 + target * 0.5;
    }
    previous = gains;
    for (let k = 0; k < bins; k++) {
      // smooth over neighbouring frequencies
      const g = (gains[Math.max(0, k - 1)] + 2 * gains[k] + gains[Math.min(bins - 1, k + 1)]) / 4;
      re[k] *= g;
      im[k] *= g;
      if (k > 0 && k < SIZE / 2) {
        re[SIZE - k] = re[k];
        im[SIZE - k] = -im[k];
      }
    }
    fft(re, im, true);
    for (let i = 0; i < SIZE; i++) {
      out[start + i] += re[i] * WINDOW[i];
      norm[start + i] += WINDOW[i] * WINDOW[i];
    }
  }
  const result = new Float32Array(pcm.length);
  for (let i = 0; i < pcm.length; i++) result[i] = norm[i] > 1e-6 ? out[i] / norm[i] : 0;
  return result;
}

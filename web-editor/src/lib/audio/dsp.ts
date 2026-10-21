/**
 * Small audio helpers working on mono Float32 PCM in the range -1..1.
 */

export interface Segment {
  /** First sample, inclusive. */
  start: number;
  /** Last sample, exclusive. */
  end: number;
}

export const toDb = (amplitude: number): number => 20 * Math.log10(Math.max(amplitude, 1e-9));
export const fromDb = (db: number): number => Math.pow(10, db / 20);

/** Average of all channels. */
export function toMono(channels: Float32Array[]): Float32Array {
  if (channels.length === 1) return channels[0].slice();
  const out = new Float32Array(channels[0].length);
  for (const channel of channels) {
    for (let i = 0; i < out.length; i++) out[i] += channel[i] / channels.length;
  }
  return out;
}

export function peak(pcm: Float32Array): number {
  let max = 0;
  for (const sample of pcm) max = Math.max(max, Math.abs(sample));
  return max;
}

export function rms(pcm: Float32Array, start = 0, end = pcm.length): number {
  let sum = 0;
  for (let i = start; i < end; i++) sum += pcm[i] * pcm[i];
  return Math.sqrt(sum / Math.max(1, end - start));
}

/** RMS level per frame, in dBFS. */
export function frameLevels(pcm: Float32Array, sampleRate: number, frameMs = 10): Float32Array {
  const size = Math.max(1, Math.round((sampleRate * frameMs) / 1000));
  const frames = new Float32Array(Math.ceil(pcm.length / size));
  for (let f = 0; f < frames.length; f++) {
    frames[f] = toDb(rms(pcm, f * size, Math.min(pcm.length, (f + 1) * size)));
  }
  return frames;
}

/** A quiet percentile of the frame levels, used as background noise level. */
export function noiseFloorDb(levels: Float32Array): number {
  if (levels.length === 0) return -90;
  const sorted = Array.from(levels).sort((a, b) => a - b);
  return sorted[Math.floor(sorted.length * 0.1)];
}

export interface SegmentOptions {
  /** Level above which a frame counts as speech; default: noise floor + 12 dB, at least -50 dBFS. */
  thresholdDb?: number;
  /** Pauses shorter than this stay inside one segment. */
  minSilenceMs?: number;
  /** Shorter sounds (clicks) are ignored. */
  minSpeechMs?: number;
  /** Silence kept before and after each segment. */
  paddingMs?: number;
}

export function speechThreshold(levels: Float32Array): number {
  return Math.max(-50, noiseFloorDb(levels) + 12);
}

/**
 * Finds the parts of a recording that contain speech.
 */
export function detectSegments(pcm: Float32Array, sampleRate: number, options: SegmentOptions = {}): Segment[] {
  const frameMs = 10;
  const frameSize = Math.round((sampleRate * frameMs) / 1000);
  const levels = frameLevels(pcm, sampleRate, frameMs);
  const threshold = options.thresholdDb ?? speechThreshold(levels);
  const minSilence = Math.ceil((options.minSilenceMs ?? 350) / frameMs);
  const minSpeech = Math.ceil((options.minSpeechMs ?? 120) / frameMs);
  const padding = Math.round(((options.paddingMs ?? 80) * sampleRate) / 1000);

  const segments: Segment[] = [];
  let start = -1;
  let silent = 0;
  for (let f = 0; f <= levels.length; f++) {
    const loud = f < levels.length && levels[f] > threshold;
    if (loud) {
      if (start < 0) start = f;
      silent = 0;
    } else if (start >= 0) {
      silent++;
      if (silent >= minSilence || f === levels.length) {
        const end = f - silent + 1;
        if (end - start >= minSpeech) {
          segments.push({
            start: Math.max(0, start * frameSize - padding),
            end: Math.min(pcm.length, end * frameSize + padding),
          });
        }
        start = -1;
        silent = 0;
      }
    }
  }
  return segments;
}

/** Start and end of the speech in a single line, without leading and trailing silence. */
export function trimSilence(pcm: Float32Array, sampleRate: number, options: SegmentOptions = {}): Segment {
  // real speech segments first, so a single click or noise burst before the line does not count
  const segments = detectSegments(pcm, sampleRate, options);
  if (segments.length === 0) return { start: 0, end: pcm.length };
  return { start: segments[0].start, end: segments[segments.length - 1].end };
}

/** Fades the first and last milliseconds in and out (raised cosine), at most a quarter of the line each. */
export function fade(pcm: Float32Array, sampleRate: number, ms: number): Float32Array {
  const out = pcm.slice();
  const length = Math.min(Math.round((ms / 1000) * sampleRate), Math.floor(pcm.length / 4));
  for (let i = 0; i < length; i++) {
    const gain = 0.5 - 0.5 * Math.cos((Math.PI * i) / length);
    out[i] *= gain;
    out[out.length - 1 - i] *= gain;
  }
  return out;
}

/** Scales the audio so its peak reaches the target level. */
export function normalize(pcm: Float32Array, targetDb = -1): Float32Array {
  const max = peak(pcm);
  if (max < 1e-6) return pcm.slice();
  const gain = fromDb(targetDb) / max;
  return pcm.map((sample) => sample * gain);
}

/** Second order high-pass filter, removes rumble below the cutoff. */
export function highPass(pcm: Float32Array, sampleRate: number, cutoffHz = 80): Float32Array {
  const w0 = (2 * Math.PI * cutoffHz) / sampleRate;
  const alpha = Math.sin(w0) / (2 * Math.SQRT1_2);
  const cos = Math.cos(w0);
  const a0 = 1 + alpha;
  const b0 = (1 + cos) / 2 / a0;
  const b1 = -(1 + cos) / a0;
  const b2 = (1 + cos) / 2 / a0;
  const a1 = (-2 * cos) / a0;
  const a2 = (1 - alpha) / a0;
  const out = new Float32Array(pcm.length);
  let x1 = 0, x2 = 0, y1 = 0, y2 = 0;
  for (let i = 0; i < pcm.length; i++) {
    const x = pcm[i];
    const y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
    out[i] = y;
    x2 = x1; x1 = x; y2 = y1; y1 = y;
  }
  return out;
}

/**
 * Silences the pauses between words: frames below the threshold are attenuated, with smooth transitions.
 */
export function noiseGate(pcm: Float32Array, sampleRate: number, thresholdDb: number, reductionDb = -30): Float32Array {
  const frameSize = Math.round(sampleRate / 100);
  const levels = frameLevels(pcm, sampleRate, 10);
  const closed = fromDb(reductionDb);
  const out = new Float32Array(pcm.length);
  let gain = 1;
  for (let f = 0; f < levels.length; f++) {
    // open fast, close slowly, so word endings are not cut off
    const target = levels[f] > thresholdDb ? 1 : closed;
    const speed = target > gain ? 0.6 : 0.15;
    const from = gain;
    gain = gain + (target - gain) * speed;
    const start = f * frameSize;
    const end = Math.min(pcm.length, start + frameSize);
    for (let i = start; i < end; i++) {
      out[i] = pcm[i] * (from + ((gain - from) * (i - start)) / frameSize);
    }
  }
  return out;
}

/**
 * Gentle compression so loud and quiet lines sound similar; the result is normalized afterwards.
 */
export function compress(pcm: Float32Array, sampleRate: number, thresholdDb = -20, ratio = 3): Float32Array {
  const attack = Math.exp(-1 / (0.005 * sampleRate));
  const release = Math.exp(-1 / (0.1 * sampleRate));
  const out = new Float32Array(pcm.length);
  let envelope = 0;
  for (let i = 0; i < pcm.length; i++) {
    const level = Math.abs(pcm[i]);
    envelope = level > envelope ? attack * envelope + (1 - attack) * level : release * envelope + (1 - release) * level;
    const overDb = toDb(envelope) - thresholdDb;
    const gainDb = overDb > 0 ? -overDb * (1 - 1 / ratio) : 0;
    out[i] = pcm[i] * fromDb(gainDb);
  }
  return out;
}

/** Zero crossings of the sinc on each side; more gives a steeper filter. */
const SINC_ZEROS = 16;
/** Steps per source sample in the precomputed filter; values in between are interpolated linearly. */
const KERNEL_STEPS = 512;

/** The windowed sinc from 0 to `half` source samples, computed once per conversion instead of for every tap. */
function sincKernel(cutoff: number, half: number): Float32Array {
  // one zero more at the end, so interpolating at the last step stays in bounds
  const kernel = new Float32Array(half * KERNEL_STEPS + 2);
  for (let k = 0; k < half * KERNEL_STEPS; k++) {
    const x = k / KERNEL_STEPS;
    const arg = Math.PI * cutoff * x;
    const sinc = arg === 0 ? 1 : Math.sin(arg) / arg;
    const w = 0.42 + 0.5 * Math.cos((Math.PI * x) / half) + 0.08 * Math.cos((2 * Math.PI * x) / half);
    kernel[k] = cutoff * sinc * w;
  }
  return kernel;
}

/**
 * Changes the sample rate with a windowed-sinc filter (Blackman window). When lowering the rate, the filter also cuts
 * everything above the new Nyquist frequency, so nothing folds back as aliasing.
 */
export function resample(pcm: Float32Array, from: number, to: number): Float32Array {
  if (from === to) return pcm.slice();
  const ratio = to / from;
  const length = Math.round(pcm.length * ratio);
  const out = new Float32Array(length);
  // cutoff relative to the source Nyquist frequency, a little below the new one when downsampling
  const cutoff = ratio < 1 ? ratio * 0.95 : 1;
  const half = Math.ceil(SINC_ZEROS / cutoff);
  const kernel = sincKernel(cutoff, half);
  const end = half * KERNEL_STEPS;
  for (let i = 0; i < length; i++) {
    const position = i / ratio;
    const center = Math.floor(position);
    let sum = 0;
    for (let j = Math.max(0, center - half + 1); j <= center + half && j < pcm.length; j++) {
      const step = Math.abs(position - j) * KERNEL_STEPS;
      const k = Math.floor(step);
      if (k >= end) continue;
      sum += pcm[j] * (kernel[k] + (kernel[k + 1] - kernel[k]) * (step - k));
    }
    out[i] = sum;
  }
  return out;
}

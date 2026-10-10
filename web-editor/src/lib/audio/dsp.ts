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

/** Changes the sample rate; when lowering it, a simple low-pass avoids aliasing. */
export function resample(pcm: Float32Array, from: number, to: number): Float32Array {
  if (from === to) return pcm.slice();
  let source = pcm;
  if (to < from) {
    const taps = Math.max(1, Math.round(from / to));
    source = new Float32Array(pcm.length);
    for (let i = 0; i < pcm.length; i++) {
      let sum = 0;
      let count = 0;
      for (let k = -taps; k <= taps; k++) {
        const j = i + k;
        if (j >= 0 && j < pcm.length) {
          sum += pcm[j];
          count++;
        }
      }
      source[i] = sum / count;
    }
  }
  const length = Math.round((source.length * to) / from);
  const out = new Float32Array(length);
  const step = from / to;
  for (let i = 0; i < length; i++) {
    const position = i * step;
    const index = Math.floor(position);
    const fraction = position - index;
    const a = source[index] ?? 0;
    const b = source[Math.min(index + 1, source.length - 1)] ?? 0;
    out[i] = a + (b - a) * fraction;
  }
  return out;
}

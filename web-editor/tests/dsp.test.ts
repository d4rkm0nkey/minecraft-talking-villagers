import { describe, expect, it } from "vitest";
import { compress, detectSegments, fade, highPass, noiseGate, normalize, peak, resample, rms, toDb, toMono, trimSilence } from "../src/lib/audio/dsp";
import { fft } from "../src/lib/audio/fft";
import { noiseProfile, noiseProfileFor, spectralGate } from "../src/lib/audio/spectralGate";
import { add, concat, noise, sine } from "./helpers";

const SR = 48000;

describe("speech detection", () => {
  it("finds three words separated by pauses", () => {
    const pause = noise(0.6, SR);
    const word = (s: number) => add(sine(s, SR), noise(s, SR));
    const pcm = concat(pause, word(0.4), pause, word(0.3), pause, word(0.5), pause);

    const segments = detectSegments(pcm, SR);

    expect(segments).toHaveLength(3);
    expect(segments[0].start / SR).toBeCloseTo(0.6 - 0.08, 1);
    expect((segments[2].end - segments[2].start) / SR).toBeCloseTo(0.5 + 0.16, 1);
  });

  it("keeps short pauses inside one segment and ignores clicks", () => {
    const click = sine(0.03, SR);
    const pcm = concat(noise(0.5, SR), sine(0.3, SR), noise(0.15, SR), sine(0.3, SR), noise(0.5, SR), click, noise(0.5, SR));

    expect(detectSegments(pcm, SR)).toHaveLength(1);
  });

  it("trims leading and trailing silence", () => {
    const pcm = concat(noise(1, SR), sine(0.5, SR), noise(1, SR));

    const speech = trimSilence(pcm, SR);

    expect(speech.start / SR).toBeCloseTo(0.92, 1);
    expect(speech.end / SR).toBeCloseTo(1.58, 1);
  });

  it("returns the whole recording if nothing is loud", () => {
    expect(trimSilence(noise(1, SR), SR)).toEqual({ start: 0, end: SR });
  });
});

describe("level and filters", () => {
  it("normalizes to the target peak", () => {
    expect(toDb(peak(normalize(sine(0.5, SR, 220, 0.1), -1)))).toBeCloseTo(-1, 1);
  });

  it("mixes channels to mono", () => {
    expect(Array.from(toMono([Float32Array.of(1, 0), Float32Array.of(0, 1)]))).toEqual([0.5, 0.5]);
  });

  it("high-pass removes rumble but keeps the voice", () => {
    const rumble = highPass(sine(1, SR, 30), SR, 80);
    const voice = highPass(sine(1, SR, 400), SR, 80);
    expect(rms(rumble, SR / 2)).toBeLessThan(0.1);
    expect(rms(voice, SR / 2)).toBeGreaterThan(0.3);
  });

  it("noise gate attenuates pauses", () => {
    const pcm = concat(sine(0.5, SR), noise(0.5, SR, 0.02), sine(0.5, SR));
    const gated = noiseGate(pcm, SR, -30);
    expect(rms(gated, SR * 0.7, SR * 0.95)).toBeLessThan(rms(pcm, SR * 0.7, SR * 0.95) / 5);
    expect(rms(gated, SR * 1.2, SR * 1.4)).toBeCloseTo(rms(pcm, SR * 1.2, SR * 1.4), 1);
  });

  it("compression reduces the difference between loud and quiet parts", () => {
    const pcm = concat(sine(0.5, SR, 220, 0.9), sine(0.5, SR, 220, 0.05));
    const out = compress(pcm, SR);
    const before = rms(pcm, 0, SR / 2) / rms(pcm, SR / 2);
    const after = rms(out, SR * 0.1, SR / 2) / rms(out, SR * 0.6);
    expect(after).toBeLessThan(before);
  });

  it("resamples to the new length", () => {
    expect(resample(sine(1, 48000), 48000, 22050).length).toBe(22050);
    expect(resample(sine(1, 22050), 22050, 44100).length).toBe(44100);
  });

  it("keeps tones below the new Nyquist frequency", () => {
    const out = resample(sine(1, 48000, 1000, 0.5), 48000, 44100);
    // the middle, away from the edges where the filter has no neighbours
    expect(rms(out, 4410, 39690)).toBeCloseTo(0.5 / Math.SQRT2, 2);
    const up = resample(sine(1, 22050, 1000, 0.5), 22050, 48000);
    expect(rms(up, 4800, 43200)).toBeCloseTo(0.5 / Math.SQRT2, 2);
  });

  it("removes tones above the new Nyquist frequency instead of folding them back", () => {
    // 15 kHz would appear as a 7 kHz alias at 22.05 kHz
    const out = resample(sine(1, 44100, 15000, 0.5), 44100, 22050);
    expect(rms(out, 2205, 19845)).toBeLessThan(0.005);
  });
});

describe("spectral noise reduction", () => {
  it("measures the room tone at the sample rate of the line", () => {
    const room = { sampleRate: 48000, data: noise(2, 48000, 0.05, 3) };
    const atLineRate = noiseProfileFor(room, 44100);
    const direct = noiseProfile(resample(room.data, 48000, 44100));
    expect(Array.from(atLineRate)).toEqual(Array.from(direct));
    // computed once per sample rate
    expect(noiseProfileFor(room, 44100)).toBe(atLineRate);
  });

  it("fft round trip", () => {
    const re = Float32Array.from({ length: 64 }, (_, i) => Math.sin(i));
    const im = new Float32Array(64);
    const original = re.slice();
    fft(re, im);
    fft(re, im, true);
    re.forEach((value, i) => expect(value).toBeCloseTo(original[i], 4));
  });

  it("removes steady noise and keeps the voice", () => {
    const hiss = noise(3, SR, 0.05, 7);
    const profile = noiseProfile(noise(2, SR, 0.05, 3));
    const voice = sine(1, SR, 300, 0.4);
    const recording = add(concat(new Float32Array(SR), voice, new Float32Array(SR)), hiss);

    const cleaned = spectralGate(recording, profile, 0.8);

    expect(rms(cleaned, 0, SR * 0.9)).toBeLessThan(rms(recording, 0, SR * 0.9) / 3);
    expect(rms(cleaned, SR * 1.2, SR * 1.8)).toBeGreaterThan(rms(voice) * 0.8);
  });
});

describe("fade", () => {
  it("starts and ends silent and leaves the middle untouched", () => {
    const pcm = new Float32Array(SR).fill(0.5);

    const faded = fade(pcm, SR, 20);

    expect(faded[0]).toBe(0);
    expect(faded[faded.length - 1]).toBe(0);
    expect(faded[Math.round(0.01 * SR)]).toBeCloseTo(0.25, 2);
    expect(faded[SR / 2]).toBe(0.5);
    expect(pcm[0]).toBe(0.5);
  });

  it("never fades more than a quarter of a very short line", () => {
    const faded = fade(new Float32Array(400).fill(1), SR, 100);

    expect(faded[100]).toBe(1);
    expect(faded[299]).toBe(1);
  });
});

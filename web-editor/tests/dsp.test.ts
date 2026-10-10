import { describe, expect, it } from "vitest";
import { compress, detectSegments, highPass, noiseGate, normalize, peak, resample, rms, toDb, toMono, trimSilence } from "../src/lib/audio/dsp";
import { fft } from "../src/lib/audio/fft";
import { noiseProfile, spectralGate } from "../src/lib/audio/spectralGate";
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
});

describe("spectral noise reduction", () => {
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

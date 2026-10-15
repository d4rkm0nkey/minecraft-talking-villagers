/**
 * A voicepack project as edited in the browser. Audio is stored separately (see store/audio.ts) and referenced by id.
 */

export type Gender = "male" | "female" | "neutral";
export type Weather = "clear" | "rain" | "thunder";

export interface LineConditions {
  professions: string[];
  weather?: Weather;
}

export interface Line {
  id: string;
  event: string;
  /** Id of the recording in the audio store. */
  audioId: string;
  text: string;
  /** Trim in seconds from the start and before the end of the recording. */
  trimStart: number;
  trimEnd: number;
  conditions: LineConditions;
}

export interface Voice {
  id: string;
  name: string;
  /** Entity ids, may contain the group "illager". */
  entities: string[];
  gender: Gender;
  child: boolean;
  /** Overrides the pack language. */
  language?: string;
  pitchMin: number;
  pitchMax: number;
  lines: Line[];
}

export interface Processing {
  highPass: boolean;
  /** Neural noise suppression (RNNoise). */
  denoise: boolean;
  /** Spectral gating with the recorded noise profile. */
  spectralGate: boolean;
  spectralStrength: number;
  noiseGate: boolean;
  compress: boolean;
  normalize: boolean;
  trim: boolean;
  /** Fade in and out in milliseconds, so cuts in breaths or noise don't click; 0 turns it off. */
  fadeMs: number;
  /** Sample rate of the exported files. */
  sampleRate: 22050 | 44100 | 48000;
  /** Vorbis quality, 0 (small) to 10 (best). */
  quality: number;
}

export interface Project {
  id: string;
  packId: string;
  name: string;
  version: string;
  description: string;
  authors: string[];
  language: string;
  /** PNG data URL, 128×128. */
  icon?: string;
  voices: Voice[];
  processing: Processing;
  /** Id of the room tone recording in the audio store. */
  noiseProfileAudioId?: string;
  created: number;
  modified: number;
}

export const DEFAULT_PROCESSING: Processing = {
  highPass: true,
  denoise: true,
  spectralGate: true,
  spectralStrength: 0.7,
  noiseGate: false,
  compress: false,
  normalize: true,
  trim: true,
  fadeMs: 15,
  sampleRate: 44100,
  quality: 5,
};

export function newId(): string {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36);
}

/** Turns a name into an id like "my-pack" ([a-z0-9_-]). */
export function slugify(name: string, fallback = "voice"): string {
  const slug = name
    .toLowerCase()
    .replace(/ä/g, "ae").replace(/ö/g, "oe").replace(/ü/g, "ue").replace(/ß/g, "ss")
    .normalize("NFKD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9_-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 64);
  return slug || fallback;
}

export function newProject(name: string, language: string): Project {
  const now = Date.now();
  return {
    id: newId(),
    packId: slugify(name, "my-pack"),
    name,
    version: "1.0.0",
    description: "",
    authors: [],
    language,
    voices: [],
    processing: { ...DEFAULT_PROCESSING },
    created: now,
    modified: now,
  };
}

export function newVoice(name: string, existing: Voice[], entities: string[] = ["villager"]): Voice {
  let id = slugify(name);
  for (let n = 2; existing.some((voice) => voice.id === id); n++) id = `${slugify(name)}-${n}`;
  const gender: Gender = entities.includes("witch") ? "female" : "male";
  return { id, name, entities: [...entities], gender, child: false, pitchMin: 0.95, pitchMax: 1.05, lines: [] };
}

import { del, get, set, setMany } from "idb-keyval";
import type { Audio } from "../audio/process";
import { oggDuration } from "../audio/ogg";
import { reportError } from "./status.svelte";

/**
 * Raw recordings in IndexedDB, kept separate from the project JSON. Recordings are never changed after saving.
 * Lines from an opened pack are stored as their original Ogg file only and decoded when they are first needed.
 */
interface Stored {
  sampleRate?: number;
  data?: Float32Array;
  ogg?: Uint8Array;
  duration?: number;
}

/** An original Ogg file with its length in seconds. */
export interface Original {
  ogg: Uint8Array;
  duration: number;
}

export type Decoder = (ogg: Uint8Array) => Promise<Audio>;

/** Prefix of the IndexedDB keys of recordings. */
export const AUDIO_PREFIX = "audio:";
const key = (id: string) => `${AUDIO_PREFIX}${id}`;
const cache = new Map<string, Promise<Audio | undefined>>();
let decoder: Decoder | undefined;

/** Sets how Ogg files are decoded: Web Audio on the page, a request to the page in the export worker. */
export function setDecoder(value: Decoder): void {
  decoder = value;
}

export async function saveAudio(id: string, audio: Audio): Promise<void> {
  cache.set(id, Promise.resolve(audio));
  await set(key(id), { sampleRate: audio.sampleRate, data: audio.data, ogg: audio.ogg } satisfies Stored).catch(reportError);
}

/** Stores original Ogg files in one transaction, without decoding them. */
export async function saveOriginals(entries: { id: string; ogg: Uint8Array }[]): Promise<void> {
  await setMany(entries.map(({ id, ogg }) => [key(id), { ogg, duration: oggDuration(ogg) } satisfies Stored])).catch(reportError);
}

export function loadAudio(id: string): Promise<Audio | undefined> {
  let audio = cache.get(id);
  if (!audio) {
    audio = read(id);
    cache.set(id, audio);
    // a failed decode may work later, e.g. once the page answers the worker
    audio.catch(() => cache.delete(id));
  }
  return audio;
}

async function read(id: string): Promise<Audio | undefined> {
  const stored = (await get(key(id))) as Stored | undefined;
  if (!stored) return undefined;
  if (stored.data && stored.sampleRate) return { sampleRate: stored.sampleRate, data: stored.data, ogg: stored.ogg };
  if (!stored.ogg) return undefined;
  if (!decoder) throw new Error("No Ogg decoder available");
  return { ...(await decoder(stored.ogg)), ogg: stored.ogg };
}

/** The original Ogg file of a line from an opened pack, without decoding it. */
export async function loadOriginal(id: string): Promise<Original | undefined> {
  const stored = (await get(key(id))) as Stored | undefined;
  if (!stored?.ogg) return undefined;
  const duration = stored.duration ?? (stored.data && stored.sampleRate ? stored.data.length / stored.sampleRate : oggDuration(stored.ogg));
  return duration === undefined ? undefined : { ogg: stored.ogg, duration };
}

export async function deleteAudio(id: string): Promise<void> {
  cache.delete(id);
  await del(key(id));
}

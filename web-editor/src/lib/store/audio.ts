import { del, get, set } from "idb-keyval";
import type { Audio } from "../audio/process";

/**
 * Raw recordings in IndexedDB, kept separate from the project JSON. Recordings are never changed after saving.
 */
const key = (id: string) => `audio:${id}`;
const cache = new Map<string, Audio>();

export async function saveAudio(id: string, audio: Audio): Promise<void> {
  cache.set(id, audio);
  await set(key(id), { sampleRate: audio.sampleRate, data: audio.data });
}

export async function loadAudio(id: string): Promise<Audio | undefined> {
  const cached = cache.get(id);
  if (cached) return cached;
  const stored = (await get(key(id))) as Audio | undefined;
  if (stored) cache.set(id, stored);
  return stored;
}

export async function deleteAudio(id: string): Promise<void> {
  cache.delete(id);
  await del(key(id));
}

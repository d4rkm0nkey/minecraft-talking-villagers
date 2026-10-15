import { del, get, set } from "idb-keyval";
import { DEFAULT_PROCESSING, type Project } from "../model/types";
import { deleteAudio } from "./audio";
import { reportError, requestPersistence } from "./status.svelte";

/**
 * Projects in IndexedDB. Changes are saved automatically shortly after the last edit,
 * and right away when the page is hidden or closed.
 */
const INDEX = "projects";
const key = (id: string) => `project:${id}`;

export const projects = $state<{ list: Project[]; loaded: boolean }>({ list: [], loaded: false });
const pending = new Map<string, { project: Project; timer: ReturnType<typeof setTimeout> }>();
let writing = 0;

export async function loadProjects(): Promise<void> {
  const ids = ((await get(INDEX)) as string[] | undefined) ?? [];
  const list: Project[] = [];
  for (const id of ids) {
    const project = (await get(key(id))) as Project | undefined;
    if (project) list.push({ ...project, processing: { ...DEFAULT_PROCESSING, ...project.processing } });
  }
  projects.list = list.sort((a, b) => b.modified - a.modified);
  projects.loaded = true;
  if (list.length > 0) void requestPersistence();
}

export function findProject(id: string): Project | undefined {
  return projects.list.find((project) => project.id === id);
}

export async function addProject(project: Project): Promise<void> {
  projects.list = [project, ...projects.list];
  await save(project);
  await set(INDEX, projects.list.map((p) => p.id)).catch(reportError);
  void requestPersistence();
}

/** Call after every change; writes after 500 ms without further changes. */
export function touch(project: Project): void {
  project.modified = Date.now();
  clearTimeout(pending.get(project.id)?.timer);
  pending.set(project.id, { project, timer: setTimeout(() => void save(project), 500) });
}

/** Writes all waiting changes now. */
export function flush(): void {
  for (const { project } of pending.values()) void save(project);
}

/** True while changes are waiting or being written; leaving the page then would lose them. */
export function unsaved(): boolean {
  return pending.size > 0 || writing > 0;
}

async function save(project: Project): Promise<void> {
  const entry = pending.get(project.id);
  if (entry) clearTimeout(entry.timer);
  pending.delete(project.id);
  writing++;
  try {
    await set(key(project.id), $state.snapshot(project)).catch(reportError);
  } catch {
    // shown by reportError
  } finally {
    writing--;
  }
}

if (typeof window !== "undefined") {
  window.addEventListener("pagehide", flush);
  document.addEventListener("visibilitychange", () => document.visibilityState === "hidden" && flush());
  window.addEventListener("beforeunload", (event) => {
    flush();
    if (unsaved()) event.preventDefault();
  });
}

export async function removeProject(project: Project): Promise<void> {
  clearTimeout(pending.get(project.id)?.timer);
  pending.delete(project.id);
  projects.list = projects.list.filter((p) => p.id !== project.id);
  await set(INDEX, projects.list.map((p) => p.id));
  await del(key(project.id));
  for (const voice of project.voices) for (const line of voice.lines) await deleteAudio(line.audioId);
  if (project.noiseProfileAudioId) await deleteAudio(project.noiseProfileAudioId);
}

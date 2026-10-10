import { del, get, set } from "idb-keyval";
import { DEFAULT_PROCESSING, type Project } from "../model/types";
import { deleteAudio } from "./audio";

/**
 * Projects in IndexedDB. Changes are saved automatically shortly after the last edit.
 */
const INDEX = "projects";
const key = (id: string) => `project:${id}`;

export const projects = $state<{ list: Project[]; loaded: boolean }>({ list: [], loaded: false });
const timers = new Map<string, ReturnType<typeof setTimeout>>();

export async function loadProjects(): Promise<void> {
  const ids = ((await get(INDEX)) as string[] | undefined) ?? [];
  const list: Project[] = [];
  for (const id of ids) {
    const project = (await get(key(id))) as Project | undefined;
    if (project) list.push({ ...project, processing: { ...DEFAULT_PROCESSING, ...project.processing } });
  }
  projects.list = list.sort((a, b) => b.modified - a.modified);
  projects.loaded = true;
}

export function findProject(id: string): Project | undefined {
  return projects.list.find((project) => project.id === id);
}

export async function addProject(project: Project): Promise<void> {
  projects.list = [project, ...projects.list];
  await set(key(project.id), $state.snapshot(project));
  await set(INDEX, projects.list.map((p) => p.id));
}

/** Call after every change; writes after 500 ms without further changes. */
export function touch(project: Project): void {
  project.modified = Date.now();
  clearTimeout(timers.get(project.id));
  timers.set(project.id, setTimeout(() => void set(key(project.id), $state.snapshot(project)), 500));
}

export async function removeProject(project: Project): Promise<void> {
  projects.list = projects.list.filter((p) => p.id !== project.id);
  await set(INDEX, projects.list.map((p) => p.id));
  await del(key(project.id));
  for (const voice of project.voices) for (const line of voice.lines) await deleteAudio(line.audioId);
  if (project.noiseProfileAudioId) await deleteAudio(project.noiseProfileAudioId);
}

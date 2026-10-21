import { del, get, keys, set, update } from "idb-keyval";
import { DEFAULT_PROCESSING, type Project } from "../model/types";
import { AUDIO_PREFIX, deleteAudio } from "./audio";
import { reportError, requestPersistence } from "./status.svelte";

/**
 * Projects in IndexedDB. Changes are saved automatically shortly after the last edit,
 * and right away when the page is hidden or closed.
 */
const INDEX = "projects";
const key = (id: string) => `project:${id}`;

export const projects = $state<{ list: Project[]; loaded: boolean }>({ list: [], loaded: false });
/** Set when the open project is also open in another tab, or was saved there; edits in both would overwrite each other. */
export const tabs = $state<{ conflict: "" | "open_elsewhere" | "changed_elsewhere" }>({ conflict: "" });
const pending = new Map<string, { project: Project; timer: ReturnType<typeof setTimeout> }>();
let writing = 0;

/** Tells the other tabs of the editor about saved projects and changes to the list. */
const channel = typeof BroadcastChannel === "undefined" ? undefined : new BroadcastChannel("talkingvillagers-editor");
type TabMessage = { type: "saved"; id: string } | { type: "list" };
let openId: string | undefined;
let releaseLock: (() => void) | undefined;

async function readProject(id: string): Promise<Project | undefined> {
  const project = (await get(key(id))) as Project | undefined;
  return project && { ...project, processing: { ...DEFAULT_PROCESSING, ...project.processing } };
}

export async function loadProjects(): Promise<void> {
  const ids = ((await get(INDEX)) as string[] | undefined) ?? [];
  const list: Project[] = [];
  for (const id of ids) {
    const project = await readProject(id);
    if (project) list.push(project);
  }
  projects.list = list.sort((a, b) => b.modified - a.modified);
  projects.loaded = true;
  if (list.length > 0) void requestPersistence();
  void deleteOrphanedAudio(list);
}

/**
 * Deletes recordings no project uses, e.g. of voices deleted by older versions of the editor or of a tab closed between recording
 * and saving. Only when no other tab of the editor is open, because there a new recording may not be saved yet.
 */
async function deleteOrphanedAudio(list: Project[]): Promise<void> {
  if (typeof navigator === "undefined" || !navigator.locks) return;
  await navigator.locks.request("talkingvillagers-editor-tab", { mode: "exclusive", ifAvailable: true }, async (lock) => {
    if (!lock) return;
    const used = new Set<string>();
    for (const project of list) {
      for (const voice of project.voices) for (const line of voice.lines) used.add(line.audioId);
      if (project.noiseProfileAudioId) used.add(project.noiseProfileAudioId);
    }
    for (const stored of await keys()) {
      const id = String(stored);
      if (id.startsWith(AUDIO_PREFIX) && !used.has(id.slice(AUDIO_PREFIX.length))) await deleteAudio(id.slice(AUDIO_PREFIX.length));
    }
  }).catch(() => undefined);
  // from now on this tab counts as open for the other tabs
  void navigator.locks.request("talkingvillagers-editor-tab", { mode: "shared" }, () => new Promise<void>(() => {}));
}

/**
 * Marks a project as open in this tab. If another tab has it open too, {@link tabs} says so.
 */
export function openProject(id: string | undefined): void {
  if (id === openId) return;
  releaseLock?.();
  releaseLock = undefined;
  openId = id;
  tabs.conflict = "";
  if (!id || typeof navigator === "undefined" || !navigator.locks) return;
  navigator.locks.request(`talkingvillagers-project:${id}`, { ifAvailable: true }, (lock) => {
    if (!lock) {
      if (openId === id) tabs.conflict = "open_elsewhere";
      return;
    }
    // held until the project is closed in this tab
    return new Promise<void>((resolve) => (releaseLock = resolve));
  }).catch(() => undefined);
}

channel?.addEventListener("message", (event: MessageEvent<TabMessage>) => {
  const message = event.data;
  if (message.type === "saved" && message.id === openId) tabs.conflict = "changed_elsewhere";
  if (message.type === "list" || message.type === "saved") void refreshList();
});

/** Takes over projects added or deleted in other tabs, without replacing the ones already loaded here. */
async function refreshList(): Promise<void> {
  const ids = ((await get(INDEX)) as string[] | undefined) ?? [];
  const kept = projects.list.filter((project) => ids.includes(project.id));
  for (const id of ids) {
    if (!kept.some((project) => project.id === id)) {
      const project = await readProject(id);
      if (project) kept.push(project);
    }
  }
  projects.list = kept.sort((a, b) => b.modified - a.modified);
}

export function findProject(id: string): Project | undefined {
  return projects.list.find((project) => project.id === id);
}

export async function addProject(project: Project): Promise<void> {
  projects.list = [project, ...projects.list];
  await save(project);
  // read and written in one transaction, so a project added in another tab at the same time is not lost
  await update(INDEX, (ids: string[] | undefined) => [project.id, ...(ids ?? []).filter((id) => id !== project.id)]).catch(reportError);
  channel?.postMessage({ type: "list" } satisfies TabMessage);
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
    channel?.postMessage({ type: "saved", id: project.id } satisfies TabMessage);
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
  await update(INDEX, (ids: string[] | undefined) => (ids ?? []).filter((id) => id !== project.id));
  await del(key(project.id));
  channel?.postMessage({ type: "list" } satisfies TabMessage);
  for (const voice of project.voices) for (const line of voice.lines) await deleteAudio(line.audioId);
  if (project.noiseProfileAudioId) await deleteAudio(project.noiseProfileAudioId);
}

import type { Project } from "../model/types";
import { fileName } from "./export";
import { decodeFile, download } from "../audio/browser";
import { touch } from "../store/projects.svelte";
import { t } from "../i18n/index.svelte";

/**
 * Running and finished exports by project id. They live outside the pack view, so an export goes on while a voice is open.
 */
export type ExportState = { done: number; total: number; message?: string; error?: string };
export const exports = $state<Record<string, ExportState>>({});
const workers = new Map<string, Worker>();
/** Work to do once the running export of a project has ended, e.g. deleting recordings it may still read. */
const afterwards = new Map<string, (() => void)[]>();

export function exporting(projectId: string): boolean {
  return workers.has(projectId);
}

/** Runs `task` once no export of the project is running any more, right away if none is. */
export function afterExport(projectId: string, task: () => void): void {
  if (!exporting(projectId)) return task();
  afterwards.set(projectId, [...(afterwards.get(projectId) ?? []), task]);
}

function finish(projectId: string, worker: Worker, state: ExportState): void {
  if (workers.get(projectId) !== worker) return;
  worker.terminate();
  workers.delete(projectId);
  exports[projectId] = state;
  const tasks = afterwards.get(projectId) ?? [];
  afterwards.delete(projectId);
  for (const task of tasks) task();
}

/** Stops the export of a project, e.g. because the project was deleted. */
export function cancelExport(projectId: string): void {
  const worker = workers.get(projectId);
  if (worker) finish(projectId, worker, { done: 0, total: 0 });
  delete exports[projectId];
}

export function startExport(project: Project): void {
  const total = project.voices.reduce((sum, voice) => sum + voice.lines.length, 0);
  // the export uses the project as it is now, even if it is edited while the export runs
  const snapshot = $state.snapshot(project);
  const name = fileName(snapshot);
  const modifiedAtStart = project.modified;
  workers.get(project.id)?.terminate();
  const current = new Worker(new URL("./export.worker.ts", import.meta.url), { type: "module" });
  workers.set(project.id, current);
  exports[project.id] = { done: 0, total };
  // e.g. the worker script failed to load or threw outside of the export
  current.onerror = (event) => {
    event.preventDefault();
    finish(project.id, current, { done: 0, total, error: t("pack.export_failed", { message: event.message || t("pack.export_worker_failed") }) });
  };
  current.onmessage = (event) => {
    const data = event.data;
    if (data.type === "decode") {
      decodeFile(data.ogg.slice().buffer).then(
        (audio) => current.postMessage({ type: "decoded", id: data.id, audio }, { transfer: [audio.data.buffer] }),
        (error) => current.postMessage({ type: "decoded", id: data.id, error: String(error) }));
    }
    if (data.type === "progress" && workers.get(project.id) === current) exports[project.id] = { done: data.done, total: data.total };
    if (data.type === "done") {
      download(data.bytes, name);
      const changedDuringExport = project.modified > modifiedAtStart;
      touch(project);
      project.exported = changedDuringExport ? modifiedAtStart : project.modified;
      finish(project.id, current, { done: total, total, message: t("pack.exported") });
    }
    if (data.type === "error") {
      const message = data.key ? t(data.key, data.params) : data.message;
      finish(project.id, current, { done: 0, total, error: t("pack.export_failed", { message }) });
    }
  };
  current.postMessage({ type: "export", project: snapshot });
}

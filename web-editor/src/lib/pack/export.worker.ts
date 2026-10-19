import { exportPack, LineTooLongError } from "./export";
import { MAX_LINE_SECONDS } from "./manifest";
import { loadAudio, loadOriginal, setDecoder } from "../store/audio";
import { denoise } from "../audio/browser";
import type { Audio } from "../audio/process";
import type { Project } from "../model/types";

export type ToWorker = { type: "export"; project: Project } | { type: "decoded"; id: number; audio?: Audio; error?: string };

// Web Audio is not available in workers, so the page decodes Ogg files of opened packs for the export
const decodes = new Map<number, { resolve: (audio: Audio) => void; reject: (error: Error) => void }>();
let nextDecode = 0;
setDecoder((ogg) => new Promise((resolve, reject) => {
  const id = ++nextDecode;
  decodes.set(id, { resolve, reject });
  self.postMessage({ type: "decode", id, ogg });
}));

// Runs the export (processing and Vorbis encoding) off the main thread, so the page stays responsive.
self.onmessage = async (event: MessageEvent<ToWorker>) => {
  const message = event.data;
  if (message.type === "decoded") {
    const pending = decodes.get(message.id);
    decodes.delete(message.id);
    if (message.audio) pending?.resolve(message.audio);
    else pending?.reject(new Error(message.error ?? "decoding failed"));
    return;
  }
  try {
    const bytes = await exportPack(message.project, {
      loadAudio,
      loadOriginal,
      denoise,
      onProgress: (done, total) => self.postMessage({ type: "progress", done, total }),
    });
    self.postMessage({ type: "done", bytes }, { transfer: [bytes.buffer] });
  } catch (error) {
    const problem = error instanceof LineTooLongError
      ? { key: "problem.line_too_long", params: { voice: error.voice, event: error.event, seconds: Math.round(error.seconds), max: MAX_LINE_SECONDS } }
      : {};
    self.postMessage({ type: "error", message: String(error), ...problem });
  }
};

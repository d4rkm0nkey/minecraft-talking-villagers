import { exportPack } from "./export";
import { loadAudio } from "../store/audio";
import { denoise } from "../audio/browser";
import type { Project } from "../model/types";

// Runs the export (processing and Vorbis encoding) off the main thread, so the page stays responsive.
self.onmessage = async (event: MessageEvent<Project>) => {
  try {
    const bytes = await exportPack(event.data, {
      loadAudio,
      denoise,
      onProgress: (done, total) => self.postMessage({ type: "progress", done, total }),
    });
    self.postMessage({ type: "done", bytes }, { transfer: [bytes.buffer] });
  } catch (error) {
    self.postMessage({ type: "error", message: String(error) });
  }
};

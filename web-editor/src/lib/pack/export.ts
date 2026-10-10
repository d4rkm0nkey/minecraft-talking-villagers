import { zipSync, strToU8 } from "fflate";
import type { Line, Processing, Project } from "../model/types";
import { buildManifest, linePath } from "./manifest";
import { processLine, type Audio, type Denoiser } from "../audio/process";
import { encodeVorbis } from "../audio/encode";
import { noiseProfile } from "../audio/spectralGate";

export interface ExportOptions {
  loadAudio: (id: string) => Promise<Audio | undefined>;
  denoise?: Denoiser;
  onProgress?: (done: number, total: number) => void;
}

/**
 * Builds the .voicepack file: processes and encodes every line, writes the manifest with durations and zips it.
 */
export async function exportPack(project: Project, options: ExportOptions): Promise<Uint8Array> {
  const files: Record<string, Uint8Array> = {};
  const durations = new Map<string, number>();
  const total = project.voices.reduce((sum, voice) => sum + voice.lines.length, 0);
  let done = 0;

  let profile: Float32Array | undefined;
  if (project.noiseProfileAudioId) {
    const noise = await options.loadAudio(project.noiseProfileAudioId);
    if (noise) profile = noiseProfile(noise.data);
  }

  for (const voice of project.voices) {
    const perEvent = new Map<string, number>();
    for (const line of voice.lines) {
      const index = perEvent.get(line.event) ?? 0;
      perEvent.set(line.event, index + 1);
      const raw = await options.loadAudio(line.audioId);
      if (!raw) throw new Error(`Missing audio for line ${line.id}`);
      if (raw.ogg && isUnchanged(line, project.processing)) {
        // lines from an opened pack are copied as they are, so editing a pack does not re-encode (and degrade) them
        durations.set(line.id, raw.data.length / raw.sampleRate);
        files[linePath(voice, line, index)] = raw.ogg;
        options.onProgress?.(++done, total);
        continue;
      }
      const processed = await processLine(raw, line, project.processing, profile, options.denoise);
      durations.set(line.id, processed.data.length / processed.sampleRate);
      files[linePath(voice, line, index)] = await encodeVorbis(processed.data, processed.sampleRate, project.processing.quality);
      options.onProgress?.(++done, total);
    }
  }

  files["voicepack.json"] = strToU8(JSON.stringify(buildManifest(project, durations), null, 2));
  if (project.icon) files["icon.png"] = dataUrlToBytes(project.icon);
  return zipSync(files, { level: 0 });
}

/** No trim and no processing step that would change the audio. */
export function isUnchanged(line: Line, processing: Processing): boolean {
  return line.trimStart === 0 && line.trimEnd === 0 && !processing.highPass && !processing.denoise && !processing.spectralGate
    && !processing.trim && !processing.noiseGate && !processing.compress && !processing.normalize;
}

export function dataUrlToBytes(dataUrl: string): Uint8Array {
  const base64 = dataUrl.slice(dataUrl.indexOf(",") + 1);
  const binary = atob(base64);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

export function fileName(project: Project): string {
  return `${project.packId}-${project.version}.voicepack`;
}

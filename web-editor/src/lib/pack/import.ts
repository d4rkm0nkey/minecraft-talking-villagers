import { strFromU8, unzipSync } from "fflate";
import { saveOriginals } from "../store/audio";
import { oggDuration } from "../audio/ogg";
import { EVENTS } from "../model/catalog";
import { DEFAULT_PROCESSING, newId, newVoice, slugify, type Gender, type Line, type Project, type Voice } from "../model/types";
import { validateManifest, type Problem } from "./manifest";

/**
 * Opens existing packs for editing: version 2 .voicepack files and version 1 resource packs.
 */

const bytesToDataUrl = (bytes: Uint8Array, type: string) => {
  let binary = "";
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return `data:${type};base64,${btoa(binary)}`;
};

/** How the original files are stored; replaceable in tests, where there is no IndexedDB. */
export interface ImportServices {
  save: (entries: { id: string; ogg: Uint8Array }[]) => Promise<void>;
}

const BROWSER: ImportServices = { save: saveOriginals };

/** The opened project and what could not be taken over. Warnings use the translation keys "import.<code>". */
export interface ImportResult {
  project: Project;
  warnings: Problem[];
}

/** A file that cannot be opened at all; the code is a translation key "import.<code>". */
export class ImportError extends Error {
  constructor(readonly code: string, readonly params: Record<string, string | number> = {}) {
    super(`${code} ${JSON.stringify(params)}`);
  }
}

/**
 * Collects the Ogg files of an import. They are stored as they are, in one go, and only decoded when a line is
 * played, drawn or processed; unchanged lines are exported without re-encoding.
 */
class Originals {
  readonly entries: { id: string; ogg: Uint8Array }[] = [];

  line(bytes: Uint8Array, event: string, text: string, conditions: Line["conditions"]): Line {
    const audioId = newId();
    this.entries.push({ id: audioId, ogg: bytes.slice() });
    return { id: newId(), event, audioId, text, trimStart: 0, trimEnd: 0, length: oggDuration(bytes), conditions };
  }
}

function unzip(data: ArrayBuffer): Record<string, Uint8Array> {
  try {
    return unzipSync(new Uint8Array(data));
  } catch (error) {
    throw new ImportError("not_zip", { message: String(error) });
  }
}

/**
 * Opens a .voicepack for editing. Its audio is already processed, so all processing steps start switched off.
 * Schema errors are reported as warnings as long as the pack can still be read, like the mod tolerates unknown events.
 */
export async function importVoicepack(data: ArrayBuffer, services: ImportServices = BROWSER): Promise<ImportResult> {
  const files = unzip(data);
  if (!files["voicepack.json"]) throw new ImportError("no_manifest");
  let manifest: any;
  try {
    manifest = JSON.parse(strFromU8(files["voicepack.json"]));
  } catch (error) {
    throw new ImportError("invalid_json", { message: String(error) });
  }
  const warnings: Problem[] = validateManifest(manifest).map((problem) => ({ ...problem, severity: "warning" }));
  if (typeof manifest !== "object" || manifest === null || !Array.isArray(manifest.voices)) {
    throw new ImportError("invalid_manifest", { details: warnings.slice(0, 5).map((w) => `${w.path}: ${w.params?.message}`).join("; ") });
  }
  const now = Date.now();
  const project: Project = {
    id: newId(), packId: String(manifest.id ?? "imported-pack"), name: String(manifest.name ?? manifest.id ?? "Imported pack"),
    version: String(manifest.version ?? "1.0.0"), description: manifest.description ?? "",
    authors: Array.isArray(manifest.authors) ? manifest.authors : [], language: String(manifest.language ?? "en"), voices: [],
    // imported audio is already processed
    processing: { ...DEFAULT_PROCESSING, highPass: false, denoise: false, spectralGate: false, trim: false, normalize: false },
    created: now, modified: now,
  };
  if (files["icon.png"]) project.icon = bytesToDataUrl(files["icon.png"], "image/png");
  const originals = new Originals();
  manifest.voices.forEach((v: any, index: number) => {
    if (typeof v !== "object" || v === null) return;
    const voice: Voice = {
      id: String(v.id ?? `voice-${index + 1}`), name: String(v.name ?? v.id ?? `Voice ${index + 1}`),
      entities: Array.isArray(v.entities) ? v.entities : ["villager"], gender: v.gender ?? "neutral", child: v.child ?? false,
      language: v.language ?? undefined, pitchMin: v.pitch?.min ?? 0.95, pitchMax: v.pitch?.max ?? 1.05, lines: [],
    };
    if (typeof v.weight === "number") voice.weight = v.weight;
    if (Array.isArray(v.legacyIds) && v.legacyIds.length) voice.legacyIds = v.legacyIds.map(String);
    const lines = typeof v.lines === "object" && v.lines !== null ? (v.lines as Record<string, unknown>) : {};
    for (const [event, entries] of Object.entries(lines)) {
      if (!Array.isArray(entries)) continue;
      for (const line of entries) {
        const bytes = typeof line?.file === "string" ? files[line.file] : undefined;
        if (!bytes) {
          warnings.push({ severity: "warning", code: "missing_file", path: `voices[${index}].lines.${event}`,
            params: { voice: voice.name, event, file: String(line?.file ?? "") } });
          continue;
        }
        voice.lines.push(originals.line(bytes, event, line.text ?? "", {
          professions: line.conditions?.professions ?? [], weather: line.conditions?.weather,
        }));
      }
    }
    project.voices.push(voice);
  });
  await services.save(originals.entries);
  return { project, warnings };
}

const V1_DIR = "assets/talkingvillagers/sounds/voices/";
const V1_LANG = "assets/talkingvillagers/lang/en_us.json";
const V1_EVENTS: Record<string, string> = { herodrop: "hero_gift", level: "level_up", nope: "no", tradesuccess: "trade_success" };
const V1_PROFESSIONS: Record<string, string> = { cartograph: "cartographer" };

/**
 * Converts a version 1 resource pack: one folder per voice with sub folders per profession ("all" for every villager)
 * and event. Transcripts are taken from lang/en_us.json where available. Folder names become legacy ids, so villagers
 * in old worlds keep their voice. Files of events the mod does not know are left out with a warning each.
 */
export async function importVersion1(data: ArrayBuffer, name: string, language: string, gender: Gender,
                                     services: ImportServices = BROWSER): Promise<ImportResult> {
  const files = unzip(data);
  const now = Date.now();
  const project: Project = {
    id: newId(), packId: slugify(name, "imported-pack"), name, version: "2.0.0", description: "", authors: [], language, voices: [],
    processing: { ...DEFAULT_PROCESSING, highPass: false, denoise: false, spectralGate: false, trim: false, normalize: false },
    created: now, modified: now,
  };
  const warnings: Problem[] = [];
  let lang: Record<string, string> = {};
  if (files[V1_LANG]) {
    try {
      lang = JSON.parse(strFromU8(files[V1_LANG]));
    } catch {
      warnings.push({ severity: "warning", code: "invalid_lang", path: V1_LANG });
    }
  }
  const voices = new Map<string, Voice>();
  const originals = new Originals();
  const paths = Object.keys(files).filter((path) => path.startsWith(V1_DIR) && path.endsWith(".ogg")).sort();
  for (const path of paths) {
    const [voiceName, profession, rawEvent, file] = path.slice(V1_DIR.length).split("/");
    if (!file) continue;
    const event = V1_EVENTS[rawEvent] ?? rawEvent;
    if (!EVENTS.some((known) => known.id === event)) {
      warnings.push({ severity: "warning", code: "unknown_event", path, params: { event: rawEvent, file: path.slice(V1_DIR.length) } });
      continue;
    }
    let voice = voices.get(voiceName);
    if (!voice) {
      voice = newVoice(voiceName, [...voices.values()]);
      voice.gender = gender;
      voice.legacyIds = [voiceName];
      voices.set(voiceName, voice);
    }
    const sound = file.replace(/\d*\.ogg$/, "");
    const text = lang[`subtitles.talkingvillagers.${voiceName}_${sound}`] ?? "";
    const professions = profession === "all" ? [] : [V1_PROFESSIONS[profession] ?? profession];
    voice.lines.push(originals.line(files[path], event, text, { professions }));
  }
  project.voices = [...voices.values()];
  if (project.voices.length === 0) throw new ImportError("no_v1_voices");
  await services.save(originals.entries);
  return { project, warnings };
}

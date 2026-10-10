import { strFromU8, unzipSync } from "fflate";
import { decodeFile } from "../audio/browser";
import { saveAudio } from "../store/audio";
import { DEFAULT_PROCESSING, newId, newVoice, slugify, type Gender, type Line, type Project, type Voice } from "../model/types";

/**
 * Opens existing packs for editing: version 2 .voicepack files and version 1 resource packs.
 */

const bytesToDataUrl = (bytes: Uint8Array, type: string) => {
  let binary = "";
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)));
  return `data:${type};base64,${btoa(binary)}`;
};

async function audioLine(bytes: Uint8Array, event: string, text: string, conditions: Line["conditions"]): Promise<Line> {
  const audioId = newId();
  await saveAudio(audioId, await decodeFile(bytes.slice().buffer));
  return { id: newId(), event, audioId, text, trimStart: 0, trimEnd: 0, conditions };
}

export async function importVoicepack(data: ArrayBuffer): Promise<Project> {
  const files = unzipSync(new Uint8Array(data));
  const manifest = JSON.parse(strFromU8(files["voicepack.json"]));
  const now = Date.now();
  const project: Project = {
    id: newId(), packId: manifest.id, name: manifest.name, version: manifest.version, description: manifest.description ?? "",
    authors: manifest.authors ?? [], language: manifest.language, voices: [],
    // imported audio is already processed
    processing: { ...DEFAULT_PROCESSING, highPass: false, denoise: false, spectralGate: false, trim: false, normalize: false },
    created: now, modified: now,
  };
  if (files["icon.png"]) project.icon = bytesToDataUrl(files["icon.png"], "image/png");
  for (const v of manifest.voices) {
    const voice: Voice = {
      id: v.id, name: v.name, entities: v.entities, gender: v.gender, child: v.child ?? false, language: v.language ?? undefined,
      pitchMin: v.pitch?.min ?? 0.95, pitchMax: v.pitch?.max ?? 1.05, lines: [],
    };
    for (const [event, lines] of Object.entries(v.lines as Record<string, any[]>)) {
      for (const line of lines) {
        const bytes = files[line.file];
        if (bytes) {
          voice.lines.push(await audioLine(bytes, event, line.text ?? "", {
            professions: line.conditions?.professions ?? [], weather: line.conditions?.weather,
          }));
        }
      }
    }
    project.voices.push(voice);
  }
  return project;
}

const V1_DIR = "assets/talkingvillagers/sounds/voices/";
const V1_EVENTS: Record<string, string> = { herodrop: "hero_gift", level: "level_up", nope: "no", tradesuccess: "trade_success" };
const V1_PROFESSIONS: Record<string, string> = { cartograph: "cartographer" };

/**
 * Converts a version 1 resource pack: one folder per voice with sub folders per profession ("all" for every villager)
 * and event. Transcripts are taken from lang/en_us.json where available.
 */
export async function importVersion1(data: ArrayBuffer, name: string, language: string, gender: Gender): Promise<Project> {
  const files = unzipSync(new Uint8Array(data));
  const now = Date.now();
  const project: Project = {
    id: newId(), packId: slugify(name, "imported-pack"), name, version: "2.0.0", description: "", authors: [], language, voices: [],
    processing: { ...DEFAULT_PROCESSING, highPass: false, denoise: false, spectralGate: false, trim: false, normalize: false },
    created: now, modified: now,
  };
  const lang = files["assets/talkingvillagers/lang/en_us.json"] ? JSON.parse(strFromU8(files["assets/talkingvillagers/lang/en_us.json"])) : {};
  const voices = new Map<string, Voice>();
  const paths = Object.keys(files).filter((path) => path.startsWith(V1_DIR) && path.endsWith(".ogg")).sort();
  for (const path of paths) {
    const [voiceName, profession, rawEvent, file] = path.slice(V1_DIR.length).split("/");
    if (!file) continue;
    let voice = voices.get(voiceName);
    if (!voice) {
      voice = newVoice(voiceName, [...voices.values()]);
      voice.gender = gender;
      voices.set(voiceName, voice);
    }
    const event = V1_EVENTS[rawEvent] ?? rawEvent;
    const sound = file.replace(/\d*\.ogg$/, "");
    const text = lang[`subtitles.talkingvillagers.${voiceName}_${sound}`] ?? "";
    const professions = profession === "all" ? [] : [V1_PROFESSIONS[profession] ?? profession];
    voice.lines.push(await audioLine(files[path], event, text, { professions }));
  }
  project.voices = [...voices.values()];
  return project;
}

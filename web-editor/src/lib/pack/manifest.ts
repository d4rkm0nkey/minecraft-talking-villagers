import Ajv2020 from "ajv/dist/2020";
import schema from "$schema/voicepack.schema.json";
import type { Line, Project, Voice } from "../model/types";
import { EVENTS, expandEntities } from "../model/catalog";

/**
 * Builds the voicepack.json of a project and checks it like the mod does.
 */

export const FORMAT = 2;

export interface ManifestLine {
  file: string;
  text?: string;
  duration?: number;
  conditions?: { professions?: string[]; weather?: string };
}

/** Audio file path of a line inside the archive. */
export function linePath(voice: Voice, line: Line, index: number): string {
  return `sounds/${voice.id}/${line.event}/${String(index + 1).padStart(2, "0")}.ogg`;
}

/**
 * @param durations seconds per line id, measured on the processed audio
 */
export function buildManifest(project: Project, durations: Map<string, number> = new Map()): Record<string, unknown> {
  return {
    format: FORMAT,
    id: project.packId,
    name: project.name,
    version: project.version,
    ...(project.description ? { description: project.description } : {}),
    ...(project.authors.length ? { authors: project.authors } : {}),
    language: project.language,
    voices: project.voices.map((voice) => {
      const lines: Record<string, ManifestLine[]> = {};
      const perEvent = new Map<string, number>();
      for (const line of voice.lines) {
        const index = perEvent.get(line.event) ?? 0;
        perEvent.set(line.event, index + 1);
        const entry: ManifestLine = { file: linePath(voice, line, index) };
        if (line.text.trim()) entry.text = line.text.trim();
        const duration = durations.get(line.id);
        if (duration !== undefined) entry.duration = Math.round(duration * 100) / 100;
        if (line.conditions.professions.length || line.conditions.weather) {
          entry.conditions = {
            ...(line.conditions.professions.length ? { professions: line.conditions.professions } : {}),
            ...(line.conditions.weather ? { weather: line.conditions.weather } : {}),
          };
        }
        (lines[line.event] ??= []).push(entry);
      }
      return {
        id: voice.id,
        name: voice.name,
        entities: voice.entities,
        gender: voice.gender,
        ...(voice.child ? { child: true } : {}),
        ...(voice.language ? { language: voice.language } : {}),
        ...(voice.pitchMin !== 0.95 || voice.pitchMax !== 1.05 ? { pitch: { min: voice.pitchMin, max: voice.pitchMax } } : {}),
        lines,
      };
    }),
  };
}

export interface Problem {
  severity: "error" | "warning";
  /** Same codes as the mod's ProblemCode, used as translation keys. */
  code: string;
  path: string;
  params?: Record<string, string | number>;
}

const ajv = new Ajv2020({ allErrors: true, strict: false });
const validateSchema = ajv.compile(schema);

/**
 * Schema errors plus the warnings the mod's PackValidator reports.
 */
export function check(project: Project): Problem[] {
  const problems: Problem[] = [];
  if (!validateSchema(buildManifest(project))) {
    for (const error of validateSchema.errors ?? []) {
      problems.push({ severity: "error", code: "invalid_value", path: error.instancePath || "/", params: { message: error.message ?? "" } });
    }
  }
  const ids = new Set<string>();
  project.voices.forEach((voice, v) => {
    const path = `voices[${v}]`;
    if (ids.has(voice.id)) problems.push({ severity: "error", code: "duplicate_voice_id", path, params: { id: voice.id } });
    ids.add(voice.id);
    if (voice.lines.length === 0) {
      problems.push({ severity: "warning", code: "no_lines", path, params: { voice: voice.name } });
      return;
    }
    const events = new Set(voice.lines.map((line) => line.event));
    if (!events.has("hurt") || !events.has("death")) {
      problems.push({ severity: "warning", code: "missing_basic_event", path, params: { voice: voice.name } });
    }
    if (voice.child && expandEntities(voice.entities).some((entity) => entity !== "villager")) {
      problems.push({ severity: "warning", code: "child_voice_not_villager", path, params: { voice: voice.name } });
    }
    const kinds = expandEntities(voice.entities);
    for (const event of events) {
      const known = EVENTS.find((candidate) => candidate.id === event);
      if (known && !known.entities.some((entity) => kinds.includes(entity))) {
        problems.push({ severity: "warning", code: "event_not_used", path: `${path}.lines.${event}`, params: { voice: voice.name, event } });
      }
    }
  });
  if (project.voices.length === 0) problems.push({ severity: "error", code: "no_voices", path: "voices" });
  return problems;
}

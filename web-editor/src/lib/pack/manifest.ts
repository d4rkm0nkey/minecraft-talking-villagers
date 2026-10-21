import Ajv2020 from "ajv/dist/2020";
import schema from "$schema/voicepack.schema.json";
import type { Line, Project, Voice } from "../model/types";
import { EVENTS, expandEntities } from "../model/catalog";

/**
 * Builds the voicepack.json of a project and checks it like the mod does.
 */

export const FORMAT = 2;
/** The mod rejects the whole pack if a line is longer. */
export const MAX_LINE_SECONDS = 60;

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
        // 0 is not a valid duration; the mod measures the file itself if it is missing
        const rounded = duration === undefined ? 0 : Math.round(duration * 100) / 100;
        if (rounded > 0) entry.duration = rounded;
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
        ...(voice.weight !== undefined && voice.weight !== 1 ? { weight: voice.weight } : {}),
        ...(voice.legacyIds?.length ? { legacyIds: voice.legacyIds } : {}),
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

/** Schema errors of a voicepack.json, e.g. of a pack that is opened. */
export function validateManifest(manifest: unknown): Problem[] {
  if (validateSchema(manifest)) return [];
  return (validateSchema.errors ?? []).map((error) => ({
    severity: "error" as const, code: "invalid_value", path: error.instancePath || "/", params: { message: error.message ?? "" },
  }));
}

/**
 * Schema errors plus the warnings the mod's PackValidator reports.
 */
export function check(project: Project): Problem[] {
  const problems: Problem[] = validateManifest(buildManifest(project));
  const ids = new Set<string>();
  project.voices.forEach((voice, v) => {
    const path = `voices[${v}]`;
    if (ids.has(voice.id)) problems.push({ severity: "error", code: "duplicate_voice_id", path, params: { id: voice.id } });
    ids.add(voice.id);
    voice.lines.forEach((line, l) => {
      const seconds = (line.length ?? 0) - line.trimStart - line.trimEnd;
      if (seconds > MAX_LINE_SECONDS) {
        problems.push({ severity: "error", code: "line_too_long", path: `${path}.lines[${l}]`,
          params: { voice: voice.name, event: line.event, seconds: Math.round(seconds), max: MAX_LINE_SECONDS } });
      }
    });
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
    const missing = [...new Set(kinds.flatMap((kind) => VANILLA_EVENTS[kind] ?? []))].filter((event) => !covers(events, event));
    if (missing.length > 0) {
      problems.push({ severity: "warning", code: "missing_vanilla_events", path, params: { voice: voice.name, events: missing.join(", ") } });
    }
    const traderOnly = [...events].some((event) => {
      const known = EVENTS.find((candidate) => candidate.id === event);
      return known?.entities.includes("wandering_trader") && !known.entities.includes("villager");
    });
    if (traderOnly && kinds.includes("villager") && kinds.includes("wandering_trader")) {
      problems.push({ severity: "warning", code: "trader_voice_for_villagers", path, params: { voice: voice.name } });
    }
  });
  if (project.voices.length === 0) problems.push({ severity: "error", code: "no_voices", path: "voices" });
  return problems;
}

/**
 * Events besides hurt and death that replace a vanilla sound; without lines Minecraft plays its own sound, so the
 * entity switches between its voice and vanilla noises.
 */
const VANILLA_EVENTS: Record<string, string[]> = {
  villager: ["yes", "no"],
  wandering_trader: ["yes", "no", "drink_potion", "disappear"],
  witch: ["drink_potion", "throw_potion"],
};

/** Whether the voice has lines for the event or for a more specific event that is used instead, e.g. heal_self. */
function covers(events: Set<string>, event: string): boolean {
  return events.has(event) || EVENTS.some((candidate) => candidate.parent === event && covers(events, candidate.id));
}

import { describe, expect, it } from "vitest";
import schema from "../../schema/voicepack.schema.json";
import { buildManifest, check, linePath } from "../src/lib/pack/manifest";
import { EVENTS, eventsFor, GROUPS, groupOf } from "../src/lib/model/catalog";
import { newProject, newVoice, slugify, type Line, type Project } from "../src/lib/model/types";

function line(event: string, text = ""): Line {
  return { id: `${event}-${Math.random()}`, event, audioId: "a", text, trimStart: 0, trimEnd: 0, conditions: { professions: [] } };
}

function project(): Project {
  const p = newProject("Mein Pack", "de");
  const anna = newVoice("Anna", []);
  anna.gender = "female";
  anna.lines = [line("hurt"), line("death"), line("greeting", "Hallo!"), line("greeting", "Moin")];
  anna.lines[3].conditions = { professions: ["farmer"], weather: "rain" };
  p.voices = [anna];
  return p;
}

describe("ids", () => {
  it("slugifies names", () => {
    expect(slugify("Mein Pack")).toBe("mein-pack");
    expect(slugify("Grüße & Öl")).toBe("gruesse-oel");
    expect(slugify("!!!", "fallback")).toBe("fallback");
  });

  it("gives voices unique ids", () => {
    const first = newVoice("Anna", []);
    expect(newVoice("Anna", [first]).id).toBe("anna-2");
  });
});

describe("manifest", () => {
  it("builds a schema-valid manifest with numbered files per event", () => {
    const p = project();
    const manifest = buildManifest(p, new Map([[p.voices[0].lines[2].id, 1.234]])) as any;

    expect(check(p)).toEqual([]);
    expect(manifest.id).toBe("mein-pack");
    const greeting = manifest.voices[0].lines.greeting;
    expect(greeting[0]).toEqual({ file: "sounds/anna/greeting/01.ogg", text: "Hallo!", duration: 1.23 });
    expect(greeting[1].conditions).toEqual({ professions: ["farmer"], weather: "rain" });
    expect(linePath(p.voices[0], p.voices[0].lines[0], 0)).toBe("sounds/anna/hurt/01.ogg");
  });

  it("reports the same warnings as the mod", () => {
    const p = project();
    p.voices[0].lines = [line("greeting"), line("throw_potion")];
    p.voices[0].child = true;
    p.voices[0].entities = ["villager", "witch"];
    const empty = newVoice("Leer", p.voices);
    p.voices.push(empty);

    const codes = check(p).map((problem) => problem.code);

    expect(codes).toEqual(expect.arrayContaining(["missing_basic_event", "child_voice_not_villager", "no_lines"]));
    expect(codes).not.toContain("event_not_used");
  });

  it("reports schema errors", () => {
    const p = project();
    p.language = "deutsch";
    p.packId = "Mein Pack";

    expect(check(p).filter((problem) => problem.severity === "error").length).toBeGreaterThan(0);
    expect(check({ ...project(), voices: [] }).map((problem) => problem.code)).toContain("no_voices");
  });
});

describe("catalog", () => {
  it("matches the schema's event list", () => {
    const schemaEvents = (schema as any).$defs.event.enum as string[];
    expect(EVENTS.map((event) => event.id).sort()).toEqual([...schemaEvents].sort());
  });

  it("puts every event into a group", () => {
    for (const event of EVENTS) expect(groupOf(event.id), event.id).not.toBe("other");
    const grouped = GROUPS.flatMap((group) => group.events);
    expect(new Set(grouped).size).toBe(grouped.length);
  });

  it("offers events by entity", () => {
    const witch = eventsFor(["witch"]).map((event) => event.id);
    expect(witch).toContain("brew");
    expect(witch).not.toContain("greeting");
    expect(eventsFor(["illager"]).map((event) => event.id)).toContain("summon_vex");
  });
});

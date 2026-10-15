import { describe, expect, it } from "vitest";
import { strFromU8, unzipSync } from "fflate";
import { exportPack } from "../src/lib/pack/export";
import { importVoicepack, type ImportServices } from "../src/lib/pack/import";
import { newProject, newVoice } from "../src/lib/model/types";
import type { Audio } from "../src/lib/audio/process";
import { oggDuration } from "../src/lib/audio/ogg";
import { encodeVorbis } from "../src/lib/audio/encode";
import type { ExportOptions } from "../src/lib/pack/export";
import { sine } from "./helpers";

const SR = 44100;

function services(store: Map<string, Uint8Array>): ImportServices {
  return { save: async (entries) => entries.forEach(({ id, ogg }) => store.set(id, ogg)) };
}

/** Reads back what the import stored. Node cannot decode Ogg, so decoding returns a placeholder of the right length. */
function stored(store: Map<string, Uint8Array>): ExportOptions {
  return {
    loadOriginal: async (id) => (store.has(id) ? { ogg: store.get(id)!, duration: oggDuration(store.get(id)!)! } : undefined),
    loadAudio: async (id) => (store.has(id) ? { sampleRate: SR, data: new Float32Array(SR / 2), ogg: store.get(id) } : undefined),
  };
}

describe("editing an existing pack", () => {
  it("keeps everything when a pack is opened and exported again", async () => {
    const audio = new Map<string, Audio>([["a", { sampleRate: SR, data: sine(0.5, SR) }]]);
    const project = newProject("Rundlauf", "en-GB");
    project.description = "Original";
    project.authors = ["TeaJay"];
    project.version = "1.4.0";
    const voice = newVoice("Hilde", []);
    voice.entities = ["witch"];
    voice.gender = "female";
    voice.pitchMin = 0.9;
    voice.pitchMax = 1.1;
    voice.lines = [
      { id: "1", event: "brew", audioId: "a", text: "What a beautiful day", trimStart: 0, trimEnd: 0, conditions: { professions: [] } },
      { id: "2", event: "hurt", audioId: "a", text: "", trimStart: 0, trimEnd: 0, conditions: { professions: [], weather: "rain" } },
    ];
    project.voices = [voice];
    project.processing.denoise = false;
    const original = await exportPack(project, { loadAudio: async (id) => audio.get(id) });

    const store = new Map<string, Uint8Array>();
    const opened = await importVoicepack(original.slice().buffer, services(store));

    expect(opened.packId).toBe("rundlauf");
    expect(opened.version).toBe("1.4.0");
    expect(opened.language).toBe("en-GB");
    expect(opened.authors).toEqual(["TeaJay"]);
    expect(opened.voices[0]).toMatchObject({ id: "hilde", entities: ["witch"], gender: "female", pitchMin: 0.9, pitchMax: 1.1 });
    expect(opened.voices[0].lines.map((line) => [line.event, line.text, line.conditions.weather])).toEqual([
      ["brew", "What a beautiful day", undefined], ["hurt", "", "rain"],
    ]);

    // unchanged lines are copied byte for byte instead of being re-encoded
    const again = unzipSync(await exportPack(opened, stored(store)));
    const first = unzipSync(original);
    expect(again["sounds/hilde/brew/01.ogg"]).toEqual(first["sounds/hilde/brew/01.ogg"]);
    expect(JSON.parse(strFromU8(again["voicepack.json"])).voices[0].lines.hurt[0].conditions).toEqual({ weather: "rain" });
    expect(JSON.parse(strFromU8(again["voicepack.json"])).voices[0].lines.brew[0].duration).toBeCloseTo(0.5, 1);
  });

  it("re-encodes lines that were trimmed", async () => {
    const audio = new Map<string, Audio>([["a", { sampleRate: SR, data: sine(1, SR) }]]);
    const project = newProject("Trim", "de");
    const voice = newVoice("Anna", []);
    voice.lines = [{ id: "1", event: "hurt", audioId: "a", text: "", trimStart: 0, trimEnd: 0, conditions: { professions: [] } }];
    project.voices = [voice];
    project.processing.denoise = false;
    const original = await exportPack(project, { loadAudio: async (id) => audio.get(id) });
    const store = new Map<string, Uint8Array>();
    const opened = await importVoicepack(original.slice().buffer, services(store));

    opened.voices[0].lines[0].trimEnd = 0.2;
    const again = unzipSync(await exportPack(opened, stored(store)));

    expect(again["sounds/anna/hurt/01.ogg"]).not.toEqual(unzipSync(original)["sounds/anna/hurt/01.ogg"]);
  });
});

describe("ogg duration", () => {
  it("reads the length from the headers", async () => {
    expect(oggDuration(await encodeVorbis(sine(1.5, SR), SR))).toBeCloseTo(1.5, 2);
  });

  it("gives up on files that are not Ogg Vorbis", () => {
    expect(oggDuration(new Uint8Array(100))).toBeUndefined();
  });
});

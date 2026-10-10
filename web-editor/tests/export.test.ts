import { describe, expect, it } from "vitest";
import { unzipSync, strFromU8 } from "fflate";
import { mkdirSync, writeFileSync } from "node:fs";
import { exportPack } from "../src/lib/pack/export";
import { newProject, newVoice, type Line } from "../src/lib/model/types";
import type { Audio } from "../src/lib/audio/process";
import { add, concat, noise, sine } from "./helpers";

const SR = 48000;

describe("export", () => {
  it("writes a .voicepack that the mod accepts", async () => {
    // a word with silence and hiss around it, as recorded
    const recording: Audio = { sampleRate: SR, data: add(concat(new Float32Array(SR / 2), sine(0.6, SR, 260, 0.3), new Float32Array(SR / 2)), noise(1.6, SR, 0.01)) };
    const room: Audio = { sampleRate: SR, data: noise(2, SR, 0.01, 9) };
    const audio = new Map<string, Audio>([["rec", recording], ["room", room]]);

    const project = newProject("Export Test", "de");
    project.noiseProfileAudioId = "room";
    const voice = newVoice("Anna", []);
    const lines: Line[] = ["hurt", "death", "greeting"].map((event, i) => ({
      id: `l${i}`, event, audioId: "rec", text: event === "greeting" ? "Hallo!" : "", trimStart: 0, trimEnd: 0, conditions: { professions: [] },
    }));
    voice.lines = lines;
    project.voices = [voice];
    project.processing.denoise = false;

    let progress = 0;
    const bytes = await exportPack(project, { loadAudio: async (id) => audio.get(id), onProgress: (done) => (progress = done) });

    const files = unzipSync(bytes);
    expect(progress).toBe(3);
    expect(Object.keys(files).sort()).toEqual(["sounds/anna/death/01.ogg", "sounds/anna/greeting/01.ogg", "sounds/anna/hurt/01.ogg", "voicepack.json"]);
    const manifest = JSON.parse(strFromU8(files["voicepack.json"]));
    const greeting = manifest.voices[0].lines.greeting[0];
    // silence is trimmed: about 0.6 s of speech plus padding remain
    expect(greeting.duration).toBeGreaterThan(0.6);
    expect(greeting.duration).toBeLessThan(0.9);
    expect(String.fromCharCode(...files["sounds/anna/hurt/01.ogg"].slice(0, 4))).toBe("OggS");

    // the Java test WebEditorExportTest validates this file with the mod's PackReader
    mkdirSync("build", { recursive: true });
    writeFileSync("build/test-export.voicepack", bytes);
  });
});

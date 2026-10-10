import { newId, newProject, newVoice, type Project } from "./model/types";
import { saveAudio } from "./store/audio";
import { addProject } from "./store/projects.svelte";

/**
 * Development only (#demo-pack, #demo-voice, #demo-studio): a sample project with synthetic recordings, to look at the
 * screens without clicking through them.
 */
export async function createDemoProject(): Promise<Project> {
  const project = newProject("Demo Dorf", "de");
  project.description = "Beispielprojekt";
  project.authors = ["TeaJay"];
  const anna = newVoice("Anna", []);
  anna.gender = "female";
  const grimm = newVoice("Grimm", [anna]);
  grimm.entities = ["illager"];
  project.voices = [anna, grimm];
  const texts: [string, string][] = [["greeting", "Hallo, Reisender!"], ["greeting", "Schön, dich zu sehen."], ["hurt", ""], ["work", "Noch drei Brote."]];
  for (const [event, text] of texts) {
    const audioId = newId();
    const sampleRate = 48000;
    const data = Float32Array.from({ length: sampleRate * 2 }, (_, i) => {
      const t = i / sampleRate;
      const envelope = t > 0.4 && t < 1.5 ? Math.sin(((t - 0.4) / 1.1) * Math.PI) : 0;
      return Math.sin(2 * Math.PI * 180 * t) * 0.4 * envelope + (Math.random() - 0.5) * 0.01;
    });
    await saveAudio(audioId, { sampleRate, data });
    anna.lines.push({ id: newId(), event, audioId, text, trimStart: 0.2, trimEnd: 0.3, conditions: { professions: event === "work" ? ["farmer"] : [] } });
  }
  await addProject(project);
  return project;
}

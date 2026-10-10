<script lang="ts">
  import type { Line, Project, Voice } from "../lib/model/types";
  import type { Audio } from "../lib/audio/process";
  import { processLine } from "../lib/audio/process";
  import { noiseProfile } from "../lib/audio/spectralGate";
  import { loadAudio } from "../lib/store/audio";
  import { touch } from "../lib/store/projects.svelte";
  import { denoise, play, stop } from "../lib/audio/browser";
  import { PROFESSIONS, expandEntities } from "../lib/model/catalog";
  import Waveform from "./Waveform.svelte";
  import { t } from "../lib/i18n/index.svelte";

  let { project, voice, line, ondelete }: { project: Project; voice: Voice; line: Line; ondelete: () => void } = $props();
  let raw = $state<Audio | undefined>();
  let playing = $state<"raw" | "processed" | undefined>();
  const villager = $derived(expandEntities(voice.entities).includes("villager"));

  $effect(() => {
    void loadAudio(line.audioId).then((audio) => (raw = audio));
  });

  async function playRaw() {
    if (!raw) return;
    playing = "raw";
    play(raw, 1, () => (playing = undefined));
  }

  async function playProcessed() {
    if (!raw) return;
    playing = "processed";
    const noise = project.noiseProfileAudioId ? await loadAudio(project.noiseProfileAudioId) : undefined;
    const processed = await processLine(raw, line, $state.snapshot(project.processing), noise ? noiseProfile(noise.data) : undefined, denoise);
    const pitch = (voice.pitchMin + voice.pitchMax) / 2;
    play(processed, pitch, () => (playing = undefined));
  }

  function toggleProfession(profession: string, checked: boolean) {
    line.conditions.professions = checked
      ? [...line.conditions.professions, profession]
      : line.conditions.professions.filter((p) => p !== profession);
    touch(project);
  }
</script>

<div class="line">
  <div class="row">
    {#if playing}
      <button class="icon" onclick={() => { stop(); playing = undefined; }} aria-label={t("common.stop")}>■</button>
    {:else}
      <button class="icon" onclick={playRaw} title={t("line.raw")} aria-label={t("line.raw")}>▶</button>
      <button class="icon" onclick={playProcessed} title={t("line.processed")} aria-label={t("line.processed")}>✨▶</button>
    {/if}
    <input type="text" bind:value={line.text} placeholder={t("line.text")} oninput={() => touch(project)} style="flex: 1; min-width: 180px" />
    <button class="danger icon" onclick={ondelete} aria-label={t("common.delete")}>✕</button>
  </div>
  {#if raw}
    <Waveform audio={raw} bind:trimStart={line.trimStart} bind:trimEnd={line.trimEnd} onchange={() => touch(project)} />
  {/if}
  <div class="row small">
    {#if villager}
      <details>
        <summary class="muted">{t("line.professions")}{line.conditions.professions.length ? `: ${line.conditions.professions.map((p) => t(`profession.${p}`)).join(", ")}` : ""}</summary>
        <div class="professions">
          {#each PROFESSIONS as profession}
            <label class="inline"><input type="checkbox" checked={line.conditions.professions.includes(profession)}
                   onchange={(e) => toggleProfession(profession, e.currentTarget.checked)} />{t(`profession.${profession}`)}</label>
          {/each}
        </div>
      </details>
    {/if}
    <label class="inline muted">{t("line.weather")}
      <select value={line.conditions.weather ?? ""} style="width: auto"
              onchange={(e) => { line.conditions.weather = (e.currentTarget.value || undefined) as Line["conditions"]["weather"]; touch(project); }}>
        <option value="">{t("line.weather.any")}</option>
        <option value="clear">{t("weather.clear")}</option>
        <option value="rain">{t("weather.rain")}</option>
        <option value="thunder">{t("weather.thunder")}</option>
      </select>
    </label>
  </div>
</div>

<style>
  .line { border: 1px solid var(--border); border-radius: 8px; padding: 8px; margin: 6px 0; display: flex; flex-direction: column; gap: 6px; }
  .professions { display: flex; flex-wrap: wrap; gap: 4px 12px; padding: 6px 0; }
</style>

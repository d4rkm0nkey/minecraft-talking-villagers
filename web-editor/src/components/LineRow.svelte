<script lang="ts">
  import type { Line, Project, Voice } from "../lib/model/types";
  import type { Audio } from "../lib/audio/process";
  import { processLine } from "../lib/audio/process";
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

  let element: HTMLDivElement | undefined = $state();
  let visible = $state(false);

  // voices can have hundreds of lines, so the audio is only loaded (and decoded) once a line scrolls near the screen
  $effect(() => {
    if (!element) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        visible = true;
        observer.disconnect();
      }
    }, { rootMargin: "600px 0px" });
    observer.observe(element);
    return () => observer.disconnect();
  });

  $effect(() => {
    if (visible) void ensureLoaded();
  });

  async function ensureLoaded(): Promise<Audio | undefined> {
    raw ??= await loadAudio(line.audioId).catch(() => undefined);
    return raw;
  }

  async function playRaw() {
    if (!(await ensureLoaded()) || !raw) return;
    playing = "raw";
    play(raw, 1, () => (playing = undefined));
  }

  async function playProcessed() {
    if (!(await ensureLoaded()) || !raw) return;
    playing = "processed";
    const noise = project.noiseProfileAudioId ? await loadAudio(project.noiseProfileAudioId) : undefined;
    const processed = await processLine(raw, line, $state.snapshot(project.processing), noise, denoise);
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

<div class="line" bind:this={element}>
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
  {:else}
    <div class="placeholder"></div>
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
  .line { background: var(--bg); border: 2px solid var(--border); padding: 8px; margin: 6px 0; display: flex; flex-direction: column; gap: 6px; }
  .line:hover { border-color: var(--edge); }
  summary { font-family: inherit; }
  .professions { display: flex; flex-wrap: wrap; gap: 4px 12px; padding: 6px 0; }
  .placeholder { height: 56px; background: var(--panel); }
</style>

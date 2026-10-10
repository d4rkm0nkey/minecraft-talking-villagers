<script lang="ts">
  import type { Project } from "../lib/model/types";
  import { newVoice } from "../lib/model/types";
  import { touch } from "../lib/store/projects.svelte";
  import { loadAudio } from "../lib/store/audio";
  import { check } from "../lib/pack/manifest";
  import { fileName } from "../lib/pack/export";
  import { download } from "../lib/audio/browser";
  import { COMMON_LANGUAGES, languageName } from "../lib/model/languages";
  import Problems from "./Problems.svelte";
  import { locale, t } from "../lib/i18n/index.svelte";

  let { project, onback, onvoice }: { project: Project; onback: () => void; onvoice: (voiceId: string) => void } = $props();
  let voiceName = $state("");
  let exportState = $state<{ done: number; total: number; message?: string; error?: string } | undefined>();
  let noiseSeconds = $state<number | undefined>();

  const problems = $derived(check(project));
  const errors = $derived(problems.filter((problem) => problem.severity === "error").length);

  $effect(() => {
    const id = project.noiseProfileAudioId;
    noiseSeconds = undefined;
    if (id) void loadAudio(id).then((audio) => (noiseSeconds = audio ? Math.round((audio.data.length / audio.sampleRate) * 10) / 10 : undefined));
  });

  function addVoice() {
    const voice = newVoice(voiceName.trim(), project.voices);
    project.voices.push(voice);
    voiceName = "";
    touch(project);
    onvoice(voice.id);
  }

  function removeVoice(index: number) {
    if (!confirm(`${project.voices[index].name}: ${t("common.delete")}?`)) return;
    project.voices.splice(index, 1);
    touch(project);
  }

  async function chooseIcon(event: Event) {
    const file = (event.currentTarget as HTMLInputElement).files?.[0];
    if (!file) return;
    const image = await createImageBitmap(file);
    const canvas = document.createElement("canvas");
    canvas.width = canvas.height = 128;
    const ctx = canvas.getContext("2d")!;
    const size = Math.min(image.width, image.height);
    ctx.drawImage(image, (image.width - size) / 2, (image.height - size) / 2, size, size, 0, 0, 128, 128);
    project.icon = canvas.toDataURL("image/png");
    touch(project);
  }

  function startExport() {
    const total = project.voices.reduce((sum, voice) => sum + voice.lines.length, 0);
    exportState = { done: 0, total };
    const worker = new Worker(new URL("../lib/pack/export.worker.ts", import.meta.url), { type: "module" });
    worker.onmessage = (event) => {
      const data = event.data;
      if (data.type === "progress") exportState = { done: data.done, total: data.total };
      if (data.type === "done") {
        download(data.bytes, fileName(project));
        exportState = { done: total, total, message: t("pack.exported") };
        worker.terminate();
      }
      if (data.type === "error") {
        exportState = { done: 0, total, error: t("pack.export_failed", { message: data.message }) };
        worker.terminate();
      }
    };
    worker.postMessage($state.snapshot(project));
  }
</script>

<div class="row spread" style="margin-bottom: 12px">
  <button onclick={onback}>← {t("common.back")}</button>
  <h2 style="margin: 0">{project.name}</h2>
  <span></span>
</div>

<section class="panel">
  <h2>{t("pack.details")}</h2>
  <div class="grid">
    <label>{t("start.name")}<input type="text" bind:value={project.name} oninput={() => touch(project)} /></label>
    <label>{t("pack.id")}<input type="text" bind:value={project.packId} oninput={() => touch(project)} /></label>
    <label>{t("pack.version")}<input type="text" bind:value={project.version} oninput={() => touch(project)} /></label>
    <label>
      {t("start.language")}
      <input type="text" bind:value={project.language} list="pack-languages" oninput={() => touch(project)} />
      <datalist id="pack-languages">
        {#each COMMON_LANGUAGES as tag}<option value={tag}>{languageName(tag, locale.language)}</option>{/each}
      </datalist>
    </label>
    <label>
      {t("pack.authors")}
      <input type="text" value={project.authors.join(", ")}
             oninput={(e) => { project.authors = e.currentTarget.value.split(",").map((a) => a.trim()).filter(Boolean); touch(project); }} />
    </label>
    <label>
      {t("pack.icon")}
      <span class="row">
        {#if project.icon}<img src={project.icon} alt="" width="40" height="40" />{/if}
        <input type="file" accept="image/*" onchange={chooseIcon} />
      </span>
    </label>
  </div>
  <label style="margin-top: 8px">{t("pack.description")}<input type="text" bind:value={project.description} oninput={() => touch(project)} /></label>
</section>

<section class="panel">
  <h2>{t("pack.voices")}</h2>
  {#if project.voices.length === 0}<p class="muted">{t("pack.no_voices")}</p>{/if}
  {#each project.voices as voice, index (voice.id)}
    <div class="row spread voice">
      <span>
        <strong>{voice.name}</strong>
        <span class="muted">· {t(`gender.${voice.gender}`)}{voice.child ? ` · ${t("voice.child")}` : ""} ·
          {voice.entities.map((entity) => t(`entity.${entity}`)).join(", ")} · {voice.lines.length} ♪</span>
      </span>
      <span class="row">
        <button onclick={() => onvoice(voice.id)}>{t("pack.open_voice")}</button>
        <button class="danger icon" onclick={() => removeVoice(index)} aria-label={t("common.delete")}>✕</button>
      </span>
    </div>
  {/each}
  <div class="row" style="margin-top: 12px">
    <input type="text" bind:value={voiceName} placeholder={t("pack.voice_name")} style="max-width: 260px"
           onkeydown={(e) => e.key === "Enter" && voiceName.trim() && addVoice()} />
    <button class="primary" disabled={!voiceName.trim()} onclick={addVoice}>{t("pack.add_voice")}</button>
  </div>
</section>

<section class="panel">
  <h2>{t("pack.processing")}</h2>
  <p class="muted small">{t("pack.processing.hint")}</p>
  <div class="grid">
    <label class="inline"><input type="checkbox" bind:checked={project.processing.highPass} onchange={() => touch(project)} />{t("pack.high_pass")}</label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.denoise} onchange={() => touch(project)} />{t("pack.denoise")}</label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.spectralGate} onchange={() => touch(project)} />{t("pack.spectral_gate")}</label>
    <label>{t("pack.spectral_strength")}: {Math.round(project.processing.spectralStrength * 100)} %
      <input type="range" min="0" max="1" step="0.05" bind:value={project.processing.spectralStrength} onchange={() => touch(project)} /></label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.noiseGate} onchange={() => touch(project)} />{t("pack.noise_gate")}</label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.compress} onchange={() => touch(project)} />{t("pack.compress")}</label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.normalize} onchange={() => touch(project)} />{t("pack.normalize")}</label>
    <label class="inline"><input type="checkbox" bind:checked={project.processing.trim} onchange={() => touch(project)} />{t("pack.trim")}</label>
    <label>{t("pack.sample_rate")}
      <select bind:value={project.processing.sampleRate} onchange={() => touch(project)}>
        <option value={22050}>22,05 kHz</option><option value={44100}>44,1 kHz</option><option value={48000}>48 kHz</option>
      </select></label>
    <label>{t("pack.quality")}: {project.processing.quality}
      <input type="range" min="0" max="10" step="1" bind:value={project.processing.quality} onchange={() => touch(project)} /></label>
  </div>
  <p class="small" style="margin-bottom: 0">
    <strong>{t("pack.noise_profile")}:</strong>
    {noiseSeconds ? t("pack.noise_profile.ok", { seconds: noiseSeconds }) : t("pack.noise_profile.none")}
  </p>
</section>

<section class="panel">
  <h2>{t("pack.check")}</h2>
  <Problems {problems} />
  <div class="row" style="margin-top: 12px">
    <button class="primary" disabled={errors > 0 || (exportState && !exportState.message && !exportState.error)} onclick={startExport}>
      ⬇ {t("pack.export")}
    </button>
    {#if exportState && !exportState.message && !exportState.error}
      <progress max={exportState.total} value={exportState.done}></progress>
      <span>{t("pack.exporting", { done: exportState.done, total: exportState.total })}</span>
    {/if}
  </div>
  {#if exportState?.message}<p>{exportState.message}</p>{/if}
  {#if exportState?.error}<p class="error">{exportState.error}</p>{/if}
</section>

<style>
  .voice { padding: 6px 0; border-bottom: 1px solid var(--border); }
</style>

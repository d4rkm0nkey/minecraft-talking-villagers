<script lang="ts">
  import type { Project } from "../lib/model/types";
  import { newVoice } from "../lib/model/types";
  import { VOICE_TYPES } from "../lib/model/catalog";
  import { touch } from "../lib/store/projects.svelte";
  import { check } from "../lib/pack/manifest";
  import { fileName } from "../lib/pack/export";
  import { download } from "../lib/audio/browser";
  import { COMMON_LANGUAGES, languageName } from "../lib/model/languages";
  import Problems from "./Problems.svelte";
  import ProcessingSettings from "./ProcessingSettings.svelte";
  import { locale, t } from "../lib/i18n/index.svelte";

  let { project, onback, onvoice }: { project: Project; onback: () => void; onvoice: (voiceId: string) => void } = $props();
  let voiceName = $state("");
  let voiceType = $state("villager");
  let exportState = $state<{ done: number; total: number; message?: string; error?: string } | undefined>();

  const problems = $derived(check(project));
  const errors = $derived(problems.filter((problem) => problem.severity === "error").length);

  /** 1.2.3 becomes 1.2.4; the mod installs the newer version over the older one. */
  function bumpVersion() {
    const parts = project.version.split(/[-+]/)[0].split(".").map((part) => Number.parseInt(part, 10) || 0);
    while (parts.length < 3) parts.push(0);
    parts[parts.length - 1]++;
    project.version = parts.join(".");
    touch(project);
  }

  function addVoice() {
    const entities = VOICE_TYPES.find((type) => type.id === voiceType)?.entities ?? ["villager"];
    const voice = newVoice(voiceName.trim(), project.voices, entities);
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
    <label>
      {t("pack.version")}
      <span class="row" style="flex-wrap: nowrap">
        <input type="text" bind:value={project.version} oninput={() => touch(project)} />
        <button class="icon" title={t("pack.version.bump")} aria-label={t("pack.version.bump")} onclick={bumpVersion}>+1</button>
      </span>
    </label>
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
    <label class="inline">{t("pack.voice_for")}
      <select bind:value={voiceType}>
        {#each VOICE_TYPES as type}<option value={type.id}>{t(`entity.${type.id}`)}</option>{/each}
      </select></label>
    <button class="primary" disabled={!voiceName.trim()} onclick={addVoice}>{t("pack.add_voice")}</button>
  </div>
</section>

<section class="panel">
  <h2>{t("pack.processing")}</h2>
  <ProcessingSettings {project} />
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

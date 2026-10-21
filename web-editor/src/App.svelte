<script lang="ts">
  import { onMount } from "svelte";
  import Start from "./components/Start.svelte";
  import PackEditor from "./components/PackEditor.svelte";
  import VoiceEditor from "./components/VoiceEditor.svelte";
  import Studio from "./components/Studio.svelte";
  import { findProject, loadProjects, openProject, tabs } from "./lib/store/projects.svelte";
  import { locale, setLanguage, t } from "./lib/i18n/index.svelte";
  import { importNotes, storage } from "./lib/store/status.svelte";

  type View =
    | { name: "start" }
    | { name: "pack"; projectId: string }
    | { name: "voice"; projectId: string; voiceId: string }
    | { name: "studio"; projectId: string; voiceId: string; event: string; profession?: string };

  let view = $state<View>({ name: "start" });
  const project = $derived(view.name === "start" ? undefined : findProject(view.projectId));
  const voiceId = $derived(view.name === "voice" || view.name === "studio" ? view.voiceId : undefined);
  const voice = $derived(voiceId ? project?.voices.find((v) => v.id === voiceId) : undefined);
  // lets other tabs with the same project open know about this one
  $effect(() => openProject(project?.id));

  function importWarning(warning: (typeof importNotes.warnings)[number]): string {
    return warning.code === "invalid_value" ? t("problem.invalid_value", warning.params) : t(`import.${warning.code}`, warning.params);
  }

  onMount(async () => {
    await loadProjects();
    if (import.meta.env.DEV && location.hash.startsWith("#demo")) {
      const { createDemoProject } = await import("./lib/demo");
      const demo = await createDemoProject();
      const voiceId = demo.voices[0].id;
      if (location.hash === "#demo-pack") view = { name: "pack", projectId: demo.id };
      if (location.hash === "#demo-voice") view = { name: "voice", projectId: demo.id, voiceId };
      if (location.hash === "#demo-witch") view = { name: "voice", projectId: demo.id, voiceId: demo.voices[2].id };
      if (location.hash === "#demo-studio") view = { name: "studio", projectId: demo.id, voiceId, event: "greeting" };
    }
  });
</script>

<header class="top">
  <div class="inner">
    <div class="brand">
      <img src="{import.meta.env.BASE_URL}logo.png" alt="" width="56" height="56" />
      <div>
        <h1>{t("app.title")}</h1>
        <div class="subtitle">{t("app.subtitle")}</div>
      </div>
    </div>
    <div class="links">
      <a href="../">{t("app.wiki")}</a>
      <select value={locale.language} onchange={(e) => setLanguage(e.currentTarget.value)} aria-label="Language">
        <option value="de">Deutsch</option>
        <option value="en">English</option>
      </select>
    </div>
  </div>
</header>

<main>
  {#if storage.error}
    <div class="panel storage-error" role="alert">
      <strong class="error">⚠ {t("storage.failed")}</strong>
      {storage.error === "quota" ? t("storage.quota") : storage.error}
      <button onclick={() => (storage.error = "")}>{t("common.close")}</button>
    </div>
  {/if}
  {#if tabs.conflict && project}
    <div class="panel notice" role="alert">
      <strong class="error">⚠ {t(`tabs.${tabs.conflict}`)}</strong>
      <button onclick={() => location.reload()}>{t("tabs.reload")}</button>
    </div>
  {/if}
  {#if importNotes.warnings.length && project}
    <div class="panel notice" role="status">
      <div>
        <strong>{t("import.warnings", { count: importNotes.warnings.length })}</strong>
        <ul>
          {#each importNotes.warnings.slice(0, 20) as warning}<li>{importWarning(warning)}</li>{/each}
          {#if importNotes.warnings.length > 20}<li>…</li>{/if}
        </ul>
      </div>
      <button onclick={() => (importNotes.warnings = [])}>{t("common.close")}</button>
    </div>
  {/if}
  {#if view.name === "start" || !project}
    <Start onopen={(id) => (view = { name: "pack", projectId: id })} />
  {:else if view.name === "pack"}
    <PackEditor
      {project}
      onback={() => (view = { name: "start" })}
      onvoice={(voiceId) => (view = { name: "voice", projectId: project.id, voiceId })}
    />
  {:else if view.name === "voice" && voice}
    <VoiceEditor
      {project}
      {voice}
      onback={() => (view = { name: "pack", projectId: project.id })}
      onstudio={(event, profession) => (view = { name: "studio", projectId: project.id, voiceId: voice.id, event, profession })}
      onrename={(voiceId) => (view = { name: "voice", projectId: project.id, voiceId })}
    />
  {:else if view.name === "studio" && voice}
    <Studio {project} {voice} initialEvent={view.event} profession={view.profession} onback={() => (view = { name: "voice", projectId: project.id, voiceId: voice.id })} />
  {/if}
</main>

<footer class="bottom">{t("app.notice")}</footer>

<style>
  .storage-error { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; border-color: var(--danger); }
  .storage-error button { margin-left: auto; }
  .notice { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
  .notice > button { margin-left: auto; }
  .notice ul { margin: 6px 0 0; padding-left: 20px; }
</style>

<script lang="ts">
  import { addProject, projects, removeProject } from "../lib/store/projects.svelte";
  import { newProject, type Gender } from "../lib/model/types";
  import { importVersion1, importVoicepack } from "../lib/pack/import";
  import { COMMON_LANGUAGES, LANGUAGE_PATTERN, languageName } from "../lib/model/languages";
  import { locale, t } from "../lib/i18n/index.svelte";

  let { onopen }: { onopen: (id: string) => void } = $props();
  let name = $state("");
  let language = $state(locale.language);
  let gender = $state<Gender>("neutral");
  let busy = $state(false);
  let error = $state("");

  const valid = $derived(name.trim().length > 0 && LANGUAGE_PATTERN.test(language));

  async function create() {
    const project = newProject(name.trim(), language);
    await addProject(project);
    onopen(project.id);
  }

  async function importFile(event: Event, version1: boolean) {
    const file = (event.currentTarget as HTMLInputElement).files?.[0];
    if (!file) return;
    busy = true;
    error = "";
    try {
      const data = await file.arrayBuffer();
      const fallbackName = name.trim() || file.name.replace(/\.(zip|voicepack)$/, "");
      const project = version1 ? await importVersion1(data, fallbackName, language, gender) : await importVoicepack(data);
      await addProject(project);
      onopen(project.id);
    } catch (e) {
      error = String(e);
    } finally {
      busy = false;
    }
  }

  function lineCount(project: (typeof projects.list)[number]) {
    return project.voices.reduce((sum, voice) => sum + voice.lines.length, 0);
  }
</script>

<section class="panel">
  <h2>{t("start.projects")}</h2>
  {#if projects.loaded && projects.list.length === 0}
    <p class="muted">{t("start.none")}</p>
  {/if}
  {#each projects.list as project (project.id)}
    <div class="row spread project">
      <button class="link" onclick={() => onopen(project.id)}>
        {#if project.icon}<img src={project.icon} alt="" width="32" height="32" />{/if}
        <span>
          <strong>{project.name}</strong> <span class="muted">{project.version} · {project.language}</span><br />
          <span class="muted small">
            {t("start.voices", { count: project.voices.length, lines: lineCount(project) })} ·
            {t("start.modified", { date: new Date(project.modified).toLocaleString(locale.language) })}
          </span>
        </span>
      </button>
      <button class="danger" onclick={() => confirm(t("start.delete_confirm", { name: project.name })) && removeProject(project)}>
        {t("common.delete")}
      </button>
    </div>
  {/each}
  <p class="muted small">{t("start.storage")}</p>
</section>

<section class="panel">
  <h2>{t("start.new")}</h2>
  <div class="grid">
    <label>{t("start.name")}<input type="text" bind:value={name} placeholder="Mein Dorf" /></label>
    <label>
      {t("start.language")}
      <input type="text" bind:value={language} list="languages" />
      <datalist id="languages">
        {#each COMMON_LANGUAGES as tag}<option value={tag}>{languageName(tag, locale.language)}</option>{/each}
      </datalist>
    </label>
  </div>
  <div class="row" style="margin-top: 12px">
    <button class="primary" disabled={!valid} onclick={create}>{t("start.create")}</button>
    <label class="file">
      <input type="file" accept=".voicepack,.zip" onchange={(e) => importFile(e, false)} disabled={busy} />
      <span>{t("start.open")}</span>
    </label>
  </div>
  <details style="margin-top: 12px">
    <summary>{t("start.import_v1")}</summary>
    <div class="row" style="margin-top: 8px">
      <label>
        {t("start.import_v1.gender")}
        <select bind:value={gender}>
          <option value="male">{t("gender.male")}</option>
          <option value="female">{t("gender.female")}</option>
          <option value="neutral">{t("gender.neutral")}</option>
        </select>
      </label>
      <label class="file">
        <input type="file" accept=".zip" onchange={(e) => importFile(e, true)} disabled={busy || !LANGUAGE_PATTERN.test(language)} />
        <span>{t("start.import_v1")}</span>
      </label>
    </div>
  </details>
  {#if busy}<p>{t("start.importing")}</p>{/if}
  {#if error}<p class="error">{error}</p>{/if}
</section>

<style>
  .project { padding: 8px 0; border-bottom: 1px solid var(--border); }
  .link { display: flex; gap: 10px; align-items: center; text-align: left; border: none; background: none; padding: 0; flex: 1; }
  .file { flex-direction: row; }
  .file input { display: none; }
  .file span { border: 1px solid var(--border); border-radius: 6px; padding: 6px 12px; cursor: pointer; background: var(--panel); }
</style>

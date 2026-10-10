<script lang="ts">
  import type { Gender, Project, Voice } from "../lib/model/types";
  import { newId } from "../lib/model/types";
  import { ENTITIES, GROUPS, eventsFor, recommendedVariants } from "../lib/model/catalog";
  import { touch } from "../lib/store/projects.svelte";
  import { saveAudio, deleteAudio } from "../lib/store/audio";
  import { decodeFile } from "../lib/audio/browser";
  import { COMMON_LANGUAGES, languageName } from "../lib/model/languages";
  import LineRow from "./LineRow.svelte";
  import ProcessingSettings from "./ProcessingSettings.svelte";
  import { locale, t } from "../lib/i18n/index.svelte";

  let { project, voice, onback, onstudio }: { project: Project; voice: Voice; onback: () => void; onstudio: (event: string) => void } = $props();
  let showEmpty = $state(true);

  const events = $derived(eventsFor(voice.entities));
  const used = $derived(new Set(voice.lines.map((line) => line.event)));
  /** Lines for events none of the selected entities triggers, e.g. after unticking "villager". */
  const unusedLines = $derived(voice.lines.filter((line) => !events.some((event) => event.id === line.event)));
  const groups = $derived(GROUPS.map((group) => ({ id: group.id, events: events.filter((event) => group.events.includes(event.id)) }))
    .filter((group) => group.events.length > 0));

  function toggleEntity(entity: string, checked: boolean) {
    const without = voice.entities.filter((e) => e !== entity);
    voice.entities = checked ? [...without, entity] : without.length ? without : voice.entities;
    touch(project);
  }

  async function addFiles(event: string, input: HTMLInputElement) {
    for (const file of Array.from(input.files ?? [])) {
      const audioId = newId();
      await saveAudio(audioId, await decodeFile(await file.arrayBuffer()));
      voice.lines.push({ id: newId(), event, audioId, text: "", trimStart: 0, trimEnd: 0, conditions: { professions: [] } });
    }
    input.value = "";
    touch(project);
  }

  async function removeLine(id: string) {
    const index = voice.lines.findIndex((line) => line.id === id);
    if (index < 0) return;
    const [line] = voice.lines.splice(index, 1);
    await deleteAudio(line.audioId);
    touch(project);
  }
</script>

<div class="row spread" style="margin-bottom: 12px">
  <button onclick={onback}>← {project.name}</button>
  <h2 style="margin: 0">{voice.name}</h2>
  <button class="primary" onclick={() => onstudio(events[0]?.id ?? "ambient")}>🎙 {t("voice.studio")}</button>
</div>

<section class="panel">
  <div class="grid">
    <label>{t("voice.name")}<input type="text" bind:value={voice.name} oninput={() => touch(project)} /></label>
    <label>{t("voice.gender")}
      <select bind:value={voice.gender} onchange={() => touch(project)}>
        {#each ["male", "female", "neutral"] as gender}<option value={gender as Gender}>{t(`gender.${gender}`)}</option>{/each}
      </select></label>
    <label>{t("voice.language")}
      <input type="text" value={voice.language ?? ""} list="voice-languages" placeholder={t("voice.language.inherit")}
             oninput={(e) => { voice.language = e.currentTarget.value.trim() || undefined; touch(project); }} />
      <datalist id="voice-languages">{#each COMMON_LANGUAGES as tag}<option value={tag}>{languageName(tag, locale.language)}</option>{/each}</datalist>
    </label>
    <label>{t("voice.pitch")}: {voice.pitchMin.toFixed(2)} – {voice.pitchMax.toFixed(2)}
      <span class="row">
        <input type="range" min="0.5" max="2" step="0.05" bind:value={voice.pitchMin} oninput={() => { voice.pitchMax = Math.max(voice.pitchMax, voice.pitchMin); touch(project); }} />
        <input type="range" min="0.5" max="2" step="0.05" bind:value={voice.pitchMax} oninput={() => { voice.pitchMin = Math.min(voice.pitchMin, voice.pitchMax); touch(project); }} />
      </span>
    </label>
  </div>
  <div style="margin-top: 10px">
    <div class="small muted">{t("voice.entities")}</div>
    <div class="row">
      {#each [...ENTITIES, "illager"] as entity}
        <label class="inline"><input type="checkbox" checked={voice.entities.includes(entity)}
               onchange={(e) => toggleEntity(entity, e.currentTarget.checked)} />{t(`entity.${entity}`)}</label>
      {/each}
    </div>
  </div>
  {#if voice.entities.includes("villager") || voice.child}
    <!-- only villagers have children -->
    <label class="inline" style="margin-top: 10px"><input type="checkbox" bind:checked={voice.child} onchange={() => touch(project)} />{t("voice.child")}</label>
    <div class="small muted">{t("voice.child.hint")}</div>
  {/if}
</section>

<details class="panel">
  <summary><strong>{t("pack.processing")}</strong> <span class="muted small">– {t("processing.for_pack")}</span></summary>
  <ProcessingSettings {project} compact />
</details>

<div class="row spread">
  <strong>{t("voice.progress", { done: events.filter((event) => used.has(event.id)).length, total: events.length })}</strong>
  <label class="inline small"><input type="checkbox" bind:checked={showEmpty} />{t("voice.show_all")}</label>
</div>

{#if unusedLines.length > 0}
  <section class="panel unused">
    <h2>{t("voice.unused")}</h2>
    <p class="small muted">{t("voice.unused.hint")}</p>
    {#each unusedLines as line (line.id)}
      <div class="small"><strong>{line.event}</strong></div>
      <LineRow {project} {voice} {line} ondelete={() => removeLine(line.id)} />
    {/each}
  </section>
{/if}

{#each groups as group (group.id)}
  <section class="panel">
    <h2>{t(`group.${group.id}`)}</h2>
    {#each group.events.filter((event) => showEmpty || used.has(event.id)) as event (event.id)}
      {@const lines = voice.lines.filter((line) => line.event === event.id)}
      {@const recommended = recommendedVariants(event.id)}
      <details class="event" open={lines.length > 0}>
        <summary>
          <span class="row spread" style="display: inline-flex; width: calc(100% - 20px)">
            <span><strong>{event.id}</strong> <span class="muted small">– {t(`event.${event.id}`)}</span></span>
            <span class="row small">
              <span class="chip" class:warning={lines.length > 0 && lines.length < recommended}>{lines.length}/{recommended}</span>
              {#if event.parent && lines.length === 0}<span class="muted">{t("voice.fallback", { event: event.parent })}</span>{/if}
            </span>
          </span>
        </summary>
        {#each lines as line (line.id)}
          <LineRow {project} {voice} {line} ondelete={() => removeLine(line.id)} />
        {/each}
        <div class="row" style="margin: 6px 0 10px">
          <button onclick={() => onstudio(event.id)}>🎙 {t("voice.record")}</button>
          <label class="file"><input type="file" accept="audio/*" multiple onchange={(e) => addFiles(event.id, e.currentTarget)} /><span>📁 {t("voice.add_files")}</span></label>
          <span class="muted small">{t("voice.recommended", { count: recommended })}</span>
        </div>
      </details>
    {/each}
  </section>
{/each}

<style>
  .unused { border-color: var(--warning); }
  .event { border-top: 1px solid var(--border); padding: 6px 0; }
  .file { flex-direction: row; }
  .file input { display: none; }
  .file span { border: 1px solid var(--border); border-radius: 6px; padding: 6px 12px; cursor: pointer; background: var(--panel); }
</style>

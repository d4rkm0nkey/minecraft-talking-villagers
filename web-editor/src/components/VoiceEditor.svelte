<script module lang="ts">
  /** Profession filter per voice, kept while switching to the studio and back. */
  const filters = new Map<string, string>();
</script>

<script lang="ts">
  import { untrack } from "svelte";
  import type { Gender, Project, Voice } from "../lib/model/types";
  import { newId, type Line } from "../lib/model/types";
  import { ENTITIES, GROUPS, PROFESSIONS, eventsFor, expandEntities, recommendedVariants } from "../lib/model/catalog";
  import { touch } from "../lib/store/projects.svelte";
  import { saveAudio, deleteAudio } from "../lib/store/audio";
  import { decodeFile } from "../lib/audio/browser";
  import { COMMON_LANGUAGES, languageName } from "../lib/model/languages";
  import LineRow from "./LineRow.svelte";
  import ProcessingSettings from "./ProcessingSettings.svelte";
  import { locale, t } from "../lib/i18n/index.svelte";
  import { GROUP_ITEMS, item, voiceHead } from "../lib/art";

  let { project, voice, onback, onstudio, onrename }: {
    project: Project; voice: Voice; onback: () => void; onstudio: (event: string, profession?: string) => void; onrename: (voiceId: string) => void;
  } = $props();
  let showEmpty = $state(true);
  /** "" shows every line, "general" the lines without a profession, otherwise the lines for that profession. */
  let profession = $state(untrack(() => filters.get(voice.id) ?? ""));
  $effect(() => {
    filters.set(voice.id, profession);
  });

  /** The id is stored in worlds, so it only changes when the edit is finished and valid. */
  let idDraft = $state(untrack(() => voice.id));
  const idError = $derived(!/^[a-z0-9_-]{1,64}$/.test(idDraft) ? t("voice.id.invalid")
    : project.voices.some((other) => other !== voice && other.id === idDraft) ? t("voice.id.taken") : "");

  function commitId() {
    if (idError || idDraft === voice.id) return;
    // App looks the voice up by the id in its view, so after the change `voice` is undefined until onrename
    const id = idDraft;
    filters.set(id, profession);
    filters.delete(voice.id);
    voice.id = id;
    touch(project);
    onrename(id);
  }

  const villager = $derived(expandEntities(voice.entities).includes("villager"));
  const filter = $derived(villager ? profession : "");
  /** Profession that new lines get while the list is filtered. */
  const newProfession = $derived(filter && filter !== "general" ? filter : undefined);
  const visibleLines = $derived(voice.lines.filter((line) => matches(line, filter)));

  function matches(line: Line, filter: string): boolean {
    if (!filter) return true;
    if (filter === "general") return line.conditions.professions.length === 0;
    return line.conditions.professions.includes(filter);
  }

  function count(filter: string): number {
    return voice.lines.filter((line) => matches(line, filter)).length;
  }

  const events = $derived(eventsFor(voice.entities));
  const used = $derived(new Set(visibleLines.map((line) => line.event)));
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
      voice.lines.push({ id: newId(), event, audioId, text: "", trimStart: 0, trimEnd: 0, conditions: { professions: newProfession ? [newProfession] : [] } });
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

<div class="title-bar">
  <button onclick={onback}>← {project.name}</button>
  <h2><img class="head" class:witch={voice.entities[0] === "witch"} src={voiceHead(voice.entities, newProfession)} alt="" />{voice.name}</h2>
  <button class="primary" onclick={() => onstudio(events[0]?.id ?? "ambient", newProfession)}>🎙 {t("voice.studio")}</button>
</div>

<section class="panel">
  <h2><img src={item("name_tag")} alt="" />{t("voice.details")}</h2>
  <div class="grid">
    <label>{t("voice.name")}<input type="text" bind:value={voice.name} oninput={() => touch(project)} /></label>
    <label>{t("voice.id")}
      <input type="text" bind:value={idDraft} onchange={commitId} spellcheck="false" />
      {#if idError}<span class="small error">{idError}</span>{:else}<span class="small muted">{t("voice.id.hint")}</span>{/if}
    </label>
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
  <summary><img src={item("redstone")} alt="" /><strong>{t("pack.processing")}</strong> <span class="muted small">– {t("processing.for_pack")}</span></summary>
  <ProcessingSettings {project} compact />
</details>

{#if villager}
  <section class="panel filter">
    <label class="inline">{t("voice.filter")}
      <select bind:value={profession} style="width: auto">
        <option value="">{t("voice.filter.all")} ({voice.lines.length})</option>
        <option value="general">{t("voice.filter.general")} ({count("general")})</option>
        {#each PROFESSIONS as id}<option value={id}>{t(`profession.${id}`)} ({count(id)})</option>{/each}
      </select>
    </label>
    {#if newProfession}<p class="small muted">{t("voice.filter.hint", { profession: t(`profession.${newProfession}`) })}</p>{/if}
  </section>
{/if}

<div class="row spread progress">
  <strong class="pixel">{t("voice.progress", { done: events.filter((event) => used.has(event.id)).length, total: events.length })}</strong>
  <label class="inline small"><input type="checkbox" bind:checked={showEmpty} />{t("voice.show_all")}</label>
</div>

{#if unusedLines.length > 0}
  <section class="panel unused">
    <h2><img src={item("barrier")} alt="" />{t("voice.unused")}</h2>
    <p class="small muted">{t("voice.unused.hint")}</p>
    {#each unusedLines as line (line.id)}
      <div class="small"><strong>{line.event}</strong></div>
      <LineRow {project} {voice} {line} ondelete={() => removeLine(line.id)} />
    {/each}
  </section>
{/if}

{#each groups as group (group.id)}
  <section class="panel">
    <h2><img src={item(GROUP_ITEMS[group.id])} alt="" />{t(`group.${group.id}`)}</h2>
    {#each group.events.filter((event) => showEmpty || used.has(event.id)) as event (event.id)}
      {@const lines = visibleLines.filter((line) => line.event === event.id)}
      {@const recommended = recommendedVariants(event.id)}
      <details class="event" open={lines.length > 0}>
        <summary>
          <span class="row spread" style="display: inline-flex; width: calc(100% - 20px)">
            <span><strong>{event.id}</strong> <span class="muted small">– {t(`event.${event.id}`)}</span></span>
            <span class="row small">
              <span class="chip" class:warning={lines.length > 0 && lines.length < recommended} class:done={lines.length >= recommended}>{lines.length}/{recommended}</span>
              {#if event.parent && lines.length === 0}<span class="muted">{t("voice.fallback", { event: event.parent })}</span>{/if}
            </span>
          </span>
        </summary>
        {#each lines as line (line.id)}
          <LineRow {project} {voice} {line} ondelete={() => removeLine(line.id)} />
        {/each}
        <div class="row" style="margin: 6px 0 10px">
          <button onclick={() => onstudio(event.id, newProfession)}>🎙 {t("voice.record")}</button>
          <label class="file"><input type="file" accept="audio/*" multiple onchange={(e) => addFiles(event.id, e.currentTarget)} /><span class="button">📁 {t("voice.add_files")}</span></label>
          <span class="muted small">{t("voice.recommended", { count: recommended })}</span>
        </div>
      </details>
    {/each}
  </section>
{/each}

<style>
  .unused { border-color: var(--warning); }
  .filter p { margin: 6px 0 0; }
  .progress { margin-bottom: 12px; }
  .event { border-top: 2px dashed var(--border); padding: 6px 0; }
  .event:first-of-type { border-top: none; }
  .event > summary { font-family: inherit; padding: 4px 0; }
  .event > summary strong { font-family: var(--pixel); font-weight: 600; }
  .event > summary:hover strong { color: var(--accent); }
</style>

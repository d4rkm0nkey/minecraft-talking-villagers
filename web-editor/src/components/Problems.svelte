<script lang="ts">
  import type { Problem } from "../lib/pack/manifest";
  import { t } from "../lib/i18n/index.svelte";
  import { item } from "../lib/art";

  let { problems }: { problems: Problem[] } = $props();
</script>

{#if problems.length === 0}
  <p class="ok"><img src={item("emerald")} alt="" />{t("pack.check.ok")}</p>
{:else}
  <ul>
    {#each problems as problem}
      <li class={problem.severity}><img src={item(problem.severity === "error" ? "barrier" : "paper")} alt={problem.severity} />{t(`problem.${problem.code}`, { path: problem.path, ...problem.params })}</li>
    {/each}
  </ul>
{/if}

<style>
  ul { margin: 0; padding-left: 0; list-style: none; }
  li, .ok { display: flex; gap: 8px; align-items: flex-start; padding: 3px 0; }
  .ok { margin: 0; }
  img { width: 18px; height: 18px; margin-top: 2px; image-rendering: pixelated; flex-shrink: 0; }
</style>

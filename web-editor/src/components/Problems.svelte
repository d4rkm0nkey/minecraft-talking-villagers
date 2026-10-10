<script lang="ts">
  import type { Problem } from "../lib/pack/manifest";
  import { t } from "../lib/i18n/index.svelte";

  let { problems }: { problems: Problem[] } = $props();
</script>

{#if problems.length === 0}
  <p class="muted">✔ {t("pack.check.ok")}</p>
{:else}
  <ul>
    {#each problems as problem}
      <li class={problem.severity}>{problem.severity === "error" ? "✖" : "⚠"} {t(`problem.${problem.code}`, { path: problem.path, ...problem.params })}</li>
    {/each}
  </ul>
{/if}

<style>
  ul { margin: 0; padding-left: 0; list-style: none; }
  li { padding: 2px 0; }
</style>

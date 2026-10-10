<script lang="ts">
  let { peakDb, clipping }: { peakDb: number; clipping: boolean } = $props();
  // -60 dB .. 0 dB
  const width = $derived(Math.max(0, Math.min(100, ((peakDb + 60) / 60) * 100)));
</script>

<div class="meter" class:clipping role="meter" aria-valuemin={-60} aria-valuemax={0} aria-valuenow={Math.round(peakDb)} aria-label="Level">
  <div class="bar" style="width: {width}%"></div>
</div>

<style>
  .meter { height: 10px; border-radius: 5px; background: var(--bg); border: 1px solid var(--border); overflow: hidden; }
  .bar { height: 100%; background: var(--accent); transition: width 60ms linear; }
  .clipping .bar { background: var(--record); }
</style>

<script lang="ts">
  let { peakDb, clipping }: { peakDb: number; clipping: boolean } = $props();
  // -60 dB .. 0 dB
  const width = $derived(Math.max(0, Math.min(100, ((peakDb + 60) / 60) * 100)));
</script>

<div class="meter" class:clipping role="meter" aria-valuemin={-60} aria-valuemax={0} aria-valuenow={Math.round(peakDb)} aria-label="Level">
  <div class="bar" style="width: {width}%"></div>
</div>

<style>
  /* Like the experience bar: green segments in a dark frame. */
  .meter { position: relative; height: 12px; background: #1f1f1f; border: 2px solid #000; overflow: hidden; }
  .meter::after {
    content: ""; position: absolute; inset: 0;
    background: repeating-linear-gradient(90deg, transparent 0 calc(5% - 2px), #000 calc(5% - 2px) 5%);
  }
  .bar { height: 100%; background: linear-gradient(#b4ff6e, #6ccf2e 50%, #3c8f12); transition: width 60ms linear; }
  .clipping .bar { background: linear-gradient(#ff8a80, #e53935 50%, #9b1c1c); }
</style>

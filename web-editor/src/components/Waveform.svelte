<script lang="ts">
  import type { Audio } from "../lib/audio/process";

  /** Waveform with draggable trim handles; trims are seconds from start and before end. */
  let {
    audio,
    trimStart = $bindable(0),
    trimEnd = $bindable(0),
    height = 56,
    onchange,
  }: { audio: Audio; trimStart?: number; trimEnd?: number; height?: number; onchange?: () => void } = $props();

  let canvas: HTMLCanvasElement | undefined = $state();
  let width = $state(600);
  let dragging: "start" | "end" | undefined;

  const duration = $derived(audio.data.length / audio.sampleRate);

  $effect(() => {
    if (!canvas) return;
    const ctx = canvas.getContext("2d")!;
    const ratio = window.devicePixelRatio || 1;
    canvas.width = width * ratio;
    canvas.height = height * ratio;
    ctx.scale(ratio, ratio);
    const style = getComputedStyle(canvas);
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = style.getPropertyValue("--wave").trim() || "#3f7d3a";
    const samplesPerPixel = Math.max(1, Math.floor(audio.data.length / width));
    for (let x = 0; x < width; x++) {
      let min = 0;
      let max = 0;
      for (let i = x * samplesPerPixel; i < Math.min(audio.data.length, (x + 1) * samplesPerPixel); i++) {
        min = Math.min(min, audio.data[i]);
        max = Math.max(max, audio.data[i]);
      }
      ctx.fillRect(x, ((1 - max) / 2) * height, 1, Math.max(1, ((max - min) / 2) * height));
    }
    ctx.fillStyle = style.getPropertyValue("--wave-trim").trim() || "rgba(0,0,0,.1)";
    const left = (trimStart / duration) * width;
    const right = width - (trimEnd / duration) * width;
    ctx.fillRect(0, 0, left, height);
    ctx.fillRect(right, 0, width - right, height);
    ctx.fillStyle = style.getPropertyValue("--text").trim() || "#000";
    ctx.fillRect(left, 0, 2, height);
    ctx.fillRect(right - 2, 0, 2, height);
  });

  function position(event: PointerEvent): number {
    const rect = canvas!.getBoundingClientRect();
    return Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width)) * duration;
  }

  function down(event: PointerEvent) {
    const time = position(event);
    dragging = Math.abs(time - trimStart) < Math.abs(duration - trimEnd - time) ? "start" : "end";
    canvas!.setPointerCapture(event.pointerId);
    move(event);
  }

  function move(event: PointerEvent) {
    if (!dragging) return;
    const time = position(event);
    if (dragging === "start") trimStart = Math.min(time, duration - trimEnd - 0.05);
    else trimEnd = Math.min(duration - time, duration - trimStart - 0.05);
    trimStart = Math.max(0, Math.round(trimStart * 100) / 100);
    trimEnd = Math.max(0, Math.round(trimEnd * 100) / 100);
  }

  function up() {
    if (dragging) onchange?.();
    dragging = undefined;
  }
</script>

<div bind:clientWidth={width} class="wave">
  <canvas bind:this={canvas} style="width: {width}px; height: {height}px" onpointerdown={down} onpointermove={move} onpointerup={up}></canvas>
</div>

<style>
  .wave { width: 100%; cursor: ew-resize; touch-action: none; }
  canvas { display: block; border-radius: 4px; background: var(--bg); }
</style>

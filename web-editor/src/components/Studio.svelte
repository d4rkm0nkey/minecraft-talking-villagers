<script lang="ts">
  import { onDestroy } from "svelte";
  import type { Project, Voice } from "../lib/model/types";
  import { newId } from "../lib/model/types";
  import type { Audio } from "../lib/audio/process";
  import { EVENTS } from "../lib/model/catalog";
  import { Recorder, microphones } from "../lib/recorder/recorder";
  import { detectSegments } from "../lib/audio/dsp";
  import { play } from "../lib/audio/browser";
  import { saveAudio, deleteAudio } from "../lib/store/audio";
  import { touch } from "../lib/store/projects.svelte";
  import LevelMeter from "./LevelMeter.svelte";
  import { t } from "../lib/i18n/index.svelte";

  let { project, voice, initialEvent, onback }: { project: Project; voice: Voice; initialEvent: string; onback: () => void } = $props();

  type Phase = "setup" | "idle" | "countdown" | "recording" | "review" | "roomtone";
  interface Item { event: string; text: string }

  const recorder = new Recorder();
  const MAX_SECONDS = 30;
  const END_SILENCE = 0.7;

  let microphoneList = $state<MediaDeviceInfo[]>([]);
  let microphone = $state("");
  let micReady = $state(false);
  let micError = $state("");
  let script = $state("");
  let queue = $state<Item[]>([]);
  let index = $state(0);
  let phase = $state<Phase>("setup");
  let take = $state<Audio | undefined>();
  let countdown = $state(0);
  let peakDb = $state(-90);
  let clipping = $state(false);
  let recorded = $state(0);
  let message = $state("");
  let options = $state({ countdown: true, autoStop: true, autoNext: true, longTake: false });
  let timer: ReturnType<typeof setInterval> | undefined;

  const current = $derived(queue[index]);
  const next = $derived(queue[index + 1]);

  recorder.onLevel = (level) => {
    peakDb = level.peakDb;
    if (phase === "recording" && level.peakDb > -1) clipping = true;
  };

  onDestroy(() => {
    clearInterval(timer);
    recorder.close();
  });

  async function enableMicrophone() {
    micError = "";
    try {
      await recorder.open(microphone || undefined);
      microphoneList = await microphones();
      micReady = true;
    } catch {
      micError = t("studio.no_mic");
    }
  }

  function parseScript(): Item[] {
    const known = new Set(EVENTS.map((event) => event.id));
    const items = script.split("\n").map((line) => line.trim()).filter(Boolean).map((line) => {
      const match = /^([a-z_]+)\s*:\s*(.*)$/.exec(line);
      return match && known.has(match[1]) ? { event: match[1], text: match[2] } : { event: initialEvent, text: line };
    });
    return items.length ? items : [{ event: initialEvent, text: "" }];
  }

  function begin() {
    queue = parseScript();
    index = 0;
    phase = "idle";
    message = "";
  }

  function startRecording() {
    take = undefined;
    clipping = false;
    if (!options.countdown) return record();
    phase = "countdown";
    countdown = 3;
    clearInterval(timer);
    timer = setInterval(() => {
      countdown--;
      if (countdown <= 0) {
        clearInterval(timer);
        record();
      }
    }, 600);
  }

  function record() {
    recorder.start();
    phase = "recording";
    clearInterval(timer);
    timer = setInterval(() => {
      const audio = recorder.current();
      const seconds = audio.data.length / audio.sampleRate;
      if (seconds > (options.longTake ? 600 : MAX_SECONDS)) return stopRecording();
      if (!options.autoStop || options.longTake || seconds < 1) return;
      // stop once speech has ended
      const segments = detectSegments(audio.data, audio.sampleRate);
      const last = segments[segments.length - 1];
      if (last && (audio.data.length - last.end) / audio.sampleRate > END_SILENCE) stopRecording();
    }, 100);
  }

  async function stopRecording() {
    clearInterval(timer);
    const audio = recorder.stop();
    if (options.longTake) {
      await splitLongTake(audio);
      return;
    }
    take = audio;
    phase = "review";
  }

  async function addLine(audio: Audio, item: Item) {
    const audioId = newId();
    await saveAudio(audioId, audio);
    voice.lines.push({ id: newId(), event: item.event, audioId, text: item.text, trimStart: 0, trimEnd: 0, conditions: { professions: [] } });
    recorded++;
    touch(project);
  }

  async function splitLongTake(audio: Audio) {
    const segments = detectSegments(audio.data, audio.sampleRate, { minSilenceMs: 600 });
    for (const segment of segments) {
      const item = queue[index] ?? { event: queue[queue.length - 1]?.event ?? initialEvent, text: "" };
      await addLine({ sampleRate: audio.sampleRate, data: audio.data.slice(segment.start, segment.end) }, item);
      if (index < queue.length) index++;
    }
    message = t("studio.split_result", { count: segments.length });
    phase = "idle";
  }

  /** @param variant record another take of the same line */
  async function accept(variant = false) {
    if (!take || !current) return;
    await addLine(take, current);
    take = undefined;
    if (!variant) index++;
    if (index >= queue.length) {
      index = queue.length - 1;
      message = t("studio.done");
      phase = "idle";
      if (!variant) return;
    }
    if (options.autoNext) startRecording();
    else phase = "idle";
  }

  async function roomTone() {
    phase = "roomtone";
    recorder.start();
    await new Promise((resolve) => setTimeout(resolve, 2000));
    const audio = recorder.stop();
    if (project.noiseProfileAudioId) await deleteAudio(project.noiseProfileAudioId);
    const id = newId();
    await saveAudio(id, audio);
    project.noiseProfileAudioId = id;
    touch(project);
    message = t("studio.room_tone.done");
    phase = queue.length ? "idle" : "setup";
  }

  function keydown(event: KeyboardEvent) {
    const target = event.target as HTMLElement;
    if (["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName) || phase === "setup" || phase === "roomtone" || !micReady) return;
    const key = event.key;
    if (key === " ") {
      event.preventDefault();
      if (phase === "recording") void stopRecording();
      else if (phase !== "countdown") startRecording();
    } else if (key === "Enter" && phase === "review") {
      event.preventDefault();
      void accept();
    } else if ((key === "r" || key === "R") && (phase === "review" || phase === "idle")) {
      startRecording();
    } else if ((key === "p" || key === "P") && take) {
      play(take);
    } else if (key === "+" && phase === "review") {
      void accept(true);
    } else if (key === "ArrowRight" && phase !== "recording") {
      index = Math.min(queue.length - 1, index + 1);
      take = undefined;
      phase = "idle";
    } else if (key === "ArrowLeft" && phase !== "recording") {
      index = Math.max(0, index - 1);
      take = undefined;
      phase = "idle";
    }
  }
</script>

<svelte:window onkeydown={keydown} />

<div class="row spread" style="margin-bottom: 12px">
  <button onclick={onback}>← {voice.name}</button>
  <h2 style="margin: 0">{t("studio.title", { voice: voice.name })}</h2>
  <span class="muted">{recorded} ♪</span>
</div>

<section class="panel">
  <div class="row">
    <label style="flex: 1">{t("studio.microphone")}
      <select bind:value={microphone} onchange={() => micReady && enableMicrophone()}>
        <option value="">Standard</option>
        {#each microphoneList as device}<option value={device.deviceId}>{device.label || device.deviceId}</option>{/each}
      </select>
    </label>
    {#if !micReady}<button class="primary" onclick={enableMicrophone}>🎙 {t("studio.enable")}</button>{/if}
    <button disabled={!micReady || phase === "recording" || phase === "roomtone"} onclick={roomTone}>
      {phase === "roomtone" ? t("studio.room_tone.recording") : t("studio.room_tone")}
    </button>
  </div>
  {#if micReady}<div style="margin-top: 8px"><LevelMeter {peakDb} clipping={clipping && phase === "recording"} /></div>{/if}
  {#if micError}<p class="error">{micError}</p>{/if}
  <div class="row small" style="margin-top: 8px">
    <label class="inline"><input type="checkbox" bind:checked={options.countdown} />{t("studio.countdown")}</label>
    <label class="inline"><input type="checkbox" bind:checked={options.autoStop} />{t("studio.auto_stop")}</label>
    <label class="inline"><input type="checkbox" bind:checked={options.autoNext} />{t("studio.auto_next")}</label>
    <label class="inline"><input type="checkbox" bind:checked={options.longTake} />{t("studio.long_take")}</label>
  </div>
</section>

{#if phase === "setup"}
  <section class="panel">
    <h2>{t("studio.script")}</h2>
    <p class="muted small">{t("studio.script.hint", { event: initialEvent })}</p>
    <textarea bind:value={script} placeholder={"greeting: Hallo, Reisender!\ngreeting: Schön, dich zu sehen.\nhurt:\nhurt:"}></textarea>
    <div class="row" style="margin-top: 8px">
      <button class="primary" disabled={!micReady} onclick={begin}>{t("studio.start")}</button>
    </div>
  </section>
{:else}
  <section class="panel prompter" class:recording={phase === "recording"}>
    <div class="row spread small">
      <span class="chip">{current?.event}</span>
      <span class="muted">{t("studio.current", { index: index + 1, total: queue.length })}</span>
    </div>
    <div class="text">
      {#if phase === "countdown"}
        <span class="countdown">{countdown}</span>
      {:else}
        {current?.text || t(`event.${current?.event}`)}
      {/if}
    </div>
    {#if next}<div class="muted small">{t("studio.next", { text: next.text || next.event })}</div>{/if}
    <div class="status">
      {#if phase === "recording"}<span class="rec">● {t("studio.recording")}</span>
      {:else if phase === "review" && take}<span>{t("studio.take", { seconds: (take.data.length / take.sampleRate).toFixed(1) })}</span>
      {:else}<span class="muted">{t("studio.ready")}</span>{/if}
      {#if clipping}<span class="error"> · {t("studio.clipping")}</span>{/if}
    </div>
    <div class="row">
      {#if phase === "recording"}
        <button class="primary" onclick={stopRecording}>■ {t("common.stop")}</button>
      {:else}
        <button class="primary" disabled={phase === "countdown"} onclick={startRecording}>● {t("voice.record")}</button>
      {/if}
      <button disabled={phase !== "review"} onclick={() => accept()}>✔ {t("studio.accept")}</button>
      <button disabled={phase !== "review"} onclick={() => accept(true)}>＋ {t("studio.variant")}</button>
      <button disabled={!take} onclick={() => take && play(take)}>▶ {t("common.play")}</button>
      <button onclick={() => { phase = "setup"; take = undefined; }}>{t("studio.edit_script")}</button>
    </div>
    <p class="muted small">{t("studio.keys")}</p>
    {#if message}<p><strong>{message}</strong></p>{/if}
  </section>
{/if}

<style>
  .prompter { text-align: center; }
  .prompter .row { justify-content: center; }
  .prompter .row.spread { justify-content: space-between; }
  .prompter.recording { border-color: var(--record); box-shadow: 0 0 0 2px var(--record) inset; }
  .text { font-size: 2rem; line-height: 1.3; margin: 24px 0 12px; min-height: 2.6em; }
  .countdown { font-size: 3rem; font-weight: bold; }
  .status { margin: 12px 0; min-height: 1.5em; }
  .rec { color: var(--record); font-weight: bold; }
</style>

<script lang="ts">
  import type { Project } from "../lib/model/types";
  import { touch } from "../lib/store/projects.svelte";
  import { loadAudio } from "../lib/store/audio";
  import { t } from "../lib/i18n/index.svelte";

  /** @param compact without the export settings, for the voice page and the recording studio */
  let { project, compact = false }: { project: Project; compact?: boolean } = $props();
  let noiseSeconds = $state<number | undefined>();

  $effect(() => {
    const id = project.noiseProfileAudioId;
    noiseSeconds = undefined;
    if (id) void loadAudio(id).then((audio) => (noiseSeconds = audio ? Math.round((audio.data.length / audio.sampleRate) * 10) / 10 : undefined));
  });
</script>

<p class="muted small">{t("pack.processing.hint")}</p>
<div class="grid">
  <label class="inline"><input type="checkbox" bind:checked={project.processing.highPass} onchange={() => touch(project)} />{t("pack.high_pass")}</label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.denoise} onchange={() => touch(project)} />{t("pack.denoise")}</label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.spectralGate} onchange={() => touch(project)} />{t("pack.spectral_gate")}</label>
  <label>{t("pack.spectral_strength")}: {Math.round(project.processing.spectralStrength * 100)} %
    <input type="range" min="0" max="1" step="0.05" bind:value={project.processing.spectralStrength} onchange={() => touch(project)} /></label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.noiseGate} onchange={() => touch(project)} />{t("pack.noise_gate")}</label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.compress} onchange={() => touch(project)} />{t("pack.compress")}</label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.normalize} onchange={() => touch(project)} />{t("pack.normalize")}</label>
  <label class="inline"><input type="checkbox" bind:checked={project.processing.trim} onchange={() => touch(project)} />{t("pack.trim")}</label>
  {#if !compact}
    <label>{t("pack.sample_rate")}
      <select bind:value={project.processing.sampleRate} onchange={() => touch(project)}>
        <option value={22050}>22,05 kHz</option><option value={44100}>44,1 kHz</option><option value={48000}>48 kHz</option>
      </select></label>
    <label>{t("pack.quality")}: {project.processing.quality}
      <input type="range" min="0" max="10" step="1" bind:value={project.processing.quality} onchange={() => touch(project)} /></label>
  {/if}
</div>
<p class="small" style="margin-bottom: 0">
  <strong>{t("pack.noise_profile")}:</strong>
  {noiseSeconds ? t("pack.noise_profile.ok", { seconds: noiseSeconds }) : t("pack.noise_profile.none")}
</p>

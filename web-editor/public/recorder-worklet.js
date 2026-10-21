// Collects microphone samples as mono and reports the level; runs on the audio thread.
// The page chooses how the input channels become mono: "mix" averages them, a number picks one channel.
class RecorderProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.channel = "mix";
    this.running = true;
    this.port.onmessage = (event) => {
      if (event.data.type === "channel") this.channel = event.data.channel;
      if (event.data.type === "stop") this.running = false;
    };
  }

  process(inputs) {
    const channels = inputs[0];
    if (channels && channels.length > 0) {
      const length = channels[0].length;
      let samples;
      if (this.channel === "mix" || channels.length === 1) {
        samples = new Float32Array(length);
        for (const channel of channels) for (let i = 0; i < length; i++) samples[i] += channel[i] / channels.length;
      } else {
        samples = (channels[this.channel] ?? channels[0]).slice();
      }
      let peak = 0;
      let sum = 0;
      for (let i = 0; i < length; i++) {
        const value = samples[i];
        peak = Math.max(peak, Math.abs(value));
        sum += value * value;
      }
      this.port.postMessage({ samples, peak, rms: Math.sqrt(sum / length), channels: channels.length }, [samples.buffer]);
    }
    // returning false lets the browser drop the node once it is disconnected
    return this.running;
  }
}
registerProcessor("recorder", RecorderProcessor);

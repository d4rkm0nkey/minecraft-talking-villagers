// Collects microphone samples and reports the level; runs on the audio thread.
class RecorderProcessor extends AudioWorkletProcessor {
  process(inputs) {
    const channel = inputs[0] && inputs[0][0];
    if (channel) {
      let peak = 0;
      let sum = 0;
      for (let i = 0; i < channel.length; i++) {
        const value = channel[i];
        peak = Math.max(peak, Math.abs(value));
        sum += value * value;
      }
      this.port.postMessage({ samples: channel.slice(), peak, rms: Math.sqrt(sum / channel.length) });
    }
    return true;
  }
}
registerProcessor("recorder", RecorderProcessor);

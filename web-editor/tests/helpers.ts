/** Test signals. */
export function sine(seconds: number, sampleRate: number, frequency = 220, amplitude = 0.5): Float32Array {
  return Float32Array.from({ length: Math.round(seconds * sampleRate) }, (_, i) => Math.sin((2 * Math.PI * frequency * i) / sampleRate) * amplitude);
}

export function noise(seconds: number, sampleRate: number, amplitude = 0.01, seed = 1): Float32Array {
  let state = seed;
  return Float32Array.from({ length: Math.round(seconds * sampleRate) }, () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return (state / 2147483648 - 0.5) * 2 * amplitude;
  });
}

export function concat(...parts: Float32Array[]): Float32Array {
  const out = new Float32Array(parts.reduce((sum, part) => sum + part.length, 0));
  let offset = 0;
  for (const part of parts) {
    out.set(part, offset);
    offset += part.length;
  }
  return out;
}

export function add(a: Float32Array, b: Float32Array): Float32Array {
  return a.map((value, i) => value + (b[i] ?? 0));
}

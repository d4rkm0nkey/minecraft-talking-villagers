/**
 * Reads the length of an Ogg Vorbis file from its headers, without decoding it: the sample rate comes from the
 * identification header, the number of samples from the granule position of the last page.
 */
export function oggDuration(bytes: Uint8Array): number | undefined {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const header = indexOf(bytes, [0x01, 0x76, 0x6f, 0x72, 0x62, 0x69, 0x73], 0, Math.min(bytes.length, 512)); // "\x01vorbis"
  if (header < 0 || header + 16 > bytes.length) return undefined;
  const sampleRate = view.getUint32(header + 12, true);
  for (let page = bytes.length - 27; page >= 0; page--) {
    if (bytes[page] === 0x4f && bytes[page + 1] === 0x67 && bytes[page + 2] === 0x67 && bytes[page + 3] === 0x53) { // "OggS"
      const samples = Number(view.getBigUint64(page + 6, true));
      return sampleRate > 0 ? samples / sampleRate : undefined;
    }
  }
  return undefined;
}

function indexOf(bytes: Uint8Array, pattern: number[], from: number, to: number): number {
  outer: for (let i = from; i <= to - pattern.length; i++) {
    for (let j = 0; j < pattern.length; j++) if (bytes[i + j] !== pattern[j]) continue outer;
    return i;
  }
  return -1;
}

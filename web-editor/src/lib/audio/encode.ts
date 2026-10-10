import { createOggEncoder } from "wasm-media-encoders";

/**
 * Encodes mono PCM as Ogg Vorbis, the only format Minecraft plays.
 */
export async function encodeVorbis(pcm: Float32Array, sampleRate: number, quality = 5): Promise<Uint8Array> {
  const encoder = await createOggEncoder();
  encoder.configure({ sampleRate, channels: 1, vbrQuality: quality });
  const chunks: Uint8Array[] = [];
  const chunkSize = sampleRate;
  for (let start = 0; start < pcm.length; start += chunkSize) {
    // the returned buffer belongs to the encoder and must be copied
    chunks.push(encoder.encode([pcm.subarray(start, Math.min(pcm.length, start + chunkSize))]).slice());
  }
  chunks.push(encoder.finalize().slice());
  const out = new Uint8Array(chunks.reduce((sum, chunk) => sum + chunk.length, 0));
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

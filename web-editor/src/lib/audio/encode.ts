import { createOggEncoder } from "wasm-media-encoders";

type OggEncoder = Awaited<ReturnType<typeof createOggEncoder>>;

/** Created once: creating an encoder compiles its WebAssembly module, which takes longer than encoding a line. */
let encoder: Promise<OggEncoder> | undefined;
/** The encoder is reused, so encodings run one after another. */
let queue: Promise<unknown> = Promise.resolve();

/**
 * Encodes mono PCM as Ogg Vorbis, the only format Minecraft plays.
 */
export function encodeVorbis(pcm: Float32Array, sampleRate: number, quality = 5): Promise<Uint8Array> {
  const result = queue.then(() => encode(pcm, sampleRate, quality));
  queue = result.catch(() => undefined);
  return result;
}

async function encode(pcm: Float32Array, sampleRate: number, quality: number): Promise<Uint8Array> {
  encoder ??= createOggEncoder();
  const instance = await encoder.catch((error) => {
    encoder = undefined;
    throw error;
  });
  instance.configure({ sampleRate, channels: 1, vbrQuality: quality });
  const chunks: Uint8Array[] = [];
  const chunkSize = sampleRate;
  for (let start = 0; start < pcm.length; start += chunkSize) {
    // the returned buffer belongs to the encoder and must be copied
    chunks.push(instance.encode([pcm.subarray(start, Math.min(pcm.length, start + chunkSize))]).slice());
  }
  chunks.push(instance.finalize().slice());
  const out = new Uint8Array(chunks.reduce((sum, chunk) => sum + chunk.length, 0));
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

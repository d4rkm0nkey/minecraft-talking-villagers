package me.teajay.talkingvillagers.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Header information of an Ogg file, read without decoding audio.
 *
 * @param duration length in seconds, if the file contains a final granule position
 */
public record OggInfo(Codec codec, int channels, int sampleRate, Optional<Float> duration) {
    public enum Codec {
        VORBIS,
        OPUS,
        UNKNOWN
    }

    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final byte[] CAPTURE = {'O', 'g', 'g', 'S'};
    private static final int PAGE_HEADER = 27;

    /**
     * Reads the stream fully (at most 16 MiB).
     *
     * @return empty if the data is not an Ogg stream
     */
    public static Optional<OggInfo> probe(InputStream in) throws IOException {
        return probe(in.readNBytes(MAX_BYTES));
    }

    public static Optional<OggInfo> probe(byte[] data) {
        if (data.length < PAGE_HEADER || !startsWith(data, 0, CAPTURE)) {
            return Optional.empty();
        }
        int packet = PAGE_HEADER + (data[26] & 0xFF);
        if (packet >= data.length) {
            return Optional.empty();
        }
        if (startsWith(data, packet, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'}) && packet + 16 <= data.length) {
            int channels = data[packet + 11] & 0xFF;
            int sampleRate = readIntLE(data, packet + 12);
            return Optional.of(new OggInfo(Codec.VORBIS, channels, sampleRate, duration(data, sampleRate, 0)));
        }
        if (startsWith(data, packet, "OpusHead".getBytes()) && packet + 16 <= data.length) {
            int channels = data[packet + 9] & 0xFF;
            int preSkip = (data[packet + 10] & 0xFF) | (data[packet + 11] & 0xFF) << 8;
            // Opus granule positions always count 48 kHz samples
            return Optional.of(new OggInfo(Codec.OPUS, channels, 48000, duration(data, 48000, preSkip)));
        }
        return Optional.of(new OggInfo(Codec.UNKNOWN, 0, 0, Optional.empty()));
    }

    /** The granule position of the last page is the total number of samples. */
    private static Optional<Float> duration(byte[] data, int sampleRate, int skip) {
        if (sampleRate <= 0) {
            return Optional.empty();
        }
        for (int i = data.length - PAGE_HEADER; i >= 0; i--) {
            if (startsWith(data, i, CAPTURE)) {
                long granule = readLongLE(data, i + 6);
                if (granule <= 0) {
                    return Optional.empty();
                }
                return Optional.of((float) (granule - skip) / sampleRate);
            }
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (offset + prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int readIntLE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8 | (data[offset + 2] & 0xFF) << 16 | (data[offset + 3] & 0xFF) << 24;
    }

    private static long readLongLE(byte[] data, int offset) {
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            value = value << 8 | (data[offset + i] & 0xFF);
        }
        return value;
    }
}

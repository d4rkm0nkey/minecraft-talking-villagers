package me.teajay.talkingvillagers.core.model;

import java.util.Optional;

/**
 * Globally unique reference to a voice, written as {@code pack:voice}.
 */
public record VoiceRef(String packId, String voiceId) {
    public static Optional<VoiceRef> parse(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new VoiceRef(value.substring(0, separator), value.substring(separator + 1)));
    }

    @Override
    public String toString() {
        return packId + ":" + voiceId;
    }
}

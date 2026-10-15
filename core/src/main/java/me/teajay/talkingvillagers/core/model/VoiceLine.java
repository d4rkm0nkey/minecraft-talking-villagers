package me.teajay.talkingvillagers.core.model;

import java.util.Optional;

/**
 * One recorded line.
 *
 * @param file     path of the Ogg Vorbis file inside the pack archive
 * @param text     transcript, used as subtitle
 * @param duration length in seconds, used by the server to avoid overlapping lines
 */
public record VoiceLine(String file, Optional<String> text, Optional<Float> duration, LineConditions conditions) {
    public static VoiceLine of(String file) {
        return new VoiceLine(file, Optional.empty(), Optional.empty(), LineConditions.NONE);
    }
}

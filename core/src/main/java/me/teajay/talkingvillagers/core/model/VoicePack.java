package me.teajay.talkingvillagers.core.model;

import java.util.List;
import java.util.Optional;

/**
 * Contents of a {@code voicepack.json} manifest.
 */
public record VoicePack(
        int format,
        String id,
        String name,
        String version,
        Optional<String> description,
        List<String> authors,
        String language,
        List<Voice> voices
) {
    /** Manifest format version written and read by this mod version. */
    public static final int FORMAT = 2;
    public static final String FILE_EXTENSION = ".voicepack";
    public static final String MANIFEST = "voicepack.json";
    public static final String ICON = "icon.png";

    public VoicePack {
        authors = List.copyOf(authors);
        voices = List.copyOf(voices);
    }

    public Optional<Voice> voice(String voiceId) {
        return voices.stream().filter(voice -> voice.id().equals(voiceId)).findFirst();
    }

    /**
     * The voices with their pack-qualified reference and effective language.
     */
    public List<ResolvedVoice> resolvedVoices() {
        return voices.stream()
                .map(voice -> new ResolvedVoice(new VoiceRef(id, voice.id()), voice, voice.language().orElse(language)))
                .toList();
    }
}

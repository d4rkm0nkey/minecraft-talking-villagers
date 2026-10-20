package me.teajay.talkingvillagers.client;

import java.util.function.Predicate;

/**
 * Added to the vanilla sound buffer library, see {@link me.teajay.talkingvillagers.mixin.client.SoundBufferLibraryMixin}.
 */
public interface VoiceBufferCache {
    /**
     * Releases the cached voice lines of packs for which {@code keep} answers false. Client thread.
     */
    void talkingvillagers$dropVoices(Predicate<String> keep);
}

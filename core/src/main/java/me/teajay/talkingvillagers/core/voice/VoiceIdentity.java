package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.VoiceRef;

import java.util.Optional;

/**
 * What an entity sounds like. Stored on the entity and kept for its whole life.
 *
 * @param gender   the entity's gender, rolled once and kept when it grows up or gets a new voice
 * @param voice    the assigned voice, empty while no fitting voice is installed
 * @param pitch    playback pitch of the voice for this entity
 * @param child    whether the voice was assigned while the entity was a child
 * @param language language of the entity, kept when it grows up
 * @param manual   set by a command; manual voices are kept when the entity grows up
 * @param muted    set by a command; the entity keeps its vanilla sounds and never gets a voice automatically
 */
public record VoiceIdentity(Gender gender, Optional<VoiceRef> voice, float pitch, boolean child, Optional<String> language,
                            boolean manual, boolean muted) {
    public static VoiceIdentity withoutVoice(Gender gender, boolean child, Optional<String> language) {
        return new VoiceIdentity(gender, Optional.empty(), 1.0f, child, language, false, false);
    }

    public VoiceIdentity withVoice(VoiceRef ref, float pitch, boolean child, boolean manual) {
        return new VoiceIdentity(gender, Optional.of(ref), pitch, child, language, manual, false);
    }

    /** Same gender and language, but no voice, so a new one is assigned on next use. */
    public VoiceIdentity cleared() {
        return withoutVoice(gender, child, language);
    }

    /**
     * Takes over the gender of a voice set by command, so the entity's gender matches what it sounds like.
     * Neutral voices leave the gender as it is.
     */
    public VoiceIdentity adoptingGenderOf(Gender voiceGender) {
        return voiceGender == Gender.NEUTRAL ? this : new VoiceIdentity(voiceGender, voice, pitch, child, language, manual, muted);
    }

    public VoiceIdentity withGender(Gender newGender) {
        return withoutVoice(newGender, child, language);
    }

    public VoiceIdentity withLanguage(String newLanguage) {
        return withoutVoice(gender, child, Optional.of(newLanguage));
    }

    public VoiceIdentity mute() {
        return new VoiceIdentity(gender, Optional.empty(), 1.0f, child, language, false, true);
    }
}

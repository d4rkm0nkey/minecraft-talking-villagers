package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.VoiceRef;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What kind of voice an entity needs.
 *
 * @param gender         the entity's gender; empty accepts any voice
 * @param language       wanted language; empty accepts any voice
 * @param strictLanguage if true, never fall back to a voice of another language
 * @param childFallback  what to do when no child voice exists for a child
 * @param avoid          voices to skip if another one fits, e.g. the current voice when re-rolling
 * @param usage          how often each voice is already used nearby; the least used voices are preferred
 */
public record VoiceCriteria(
        EntityKind kind,
        boolean child,
        Optional<Gender> gender,
        Optional<String> language,
        boolean strictLanguage,
        ChildFallback childFallback,
        Set<VoiceRef> avoid,
        Map<VoiceRef, Integer> usage
) {
    public enum ChildFallback {
        /** Children without a child voice keep the vanilla sounds. */
        VANILLA,
        /** Children get an adult voice played with a higher pitch. */
        PITCH_UP_ADULT
    }

    public VoiceCriteria {
        avoid = Set.copyOf(avoid);
        usage = Map.copyOf(usage);
    }

    public static VoiceCriteria of(EntityKind kind, boolean child) {
        return new VoiceCriteria(kind, child, Optional.empty(), Optional.empty(), false, ChildFallback.VANILLA, Set.of(), Map.of());
    }

    public VoiceCriteria withGender(Gender gender) {
        return new VoiceCriteria(kind, child, Optional.of(gender), language, strictLanguage, childFallback, avoid, usage);
    }

    public VoiceCriteria withLanguage(String language, boolean strict) {
        return new VoiceCriteria(kind, child, gender, Optional.of(language), strict, childFallback, avoid, usage);
    }

    public VoiceCriteria withChildFallback(ChildFallback childFallback) {
        return new VoiceCriteria(kind, child, gender, language, strictLanguage, childFallback, avoid, usage);
    }

    public VoiceCriteria avoiding(Set<VoiceRef> avoid) {
        return new VoiceCriteria(kind, child, gender, language, strictLanguage, childFallback, avoid, usage);
    }

    public VoiceCriteria withUsage(Map<VoiceRef, Integer> usage) {
        return new VoiceCriteria(kind, child, gender, language, strictLanguage, childFallback, avoid, usage);
    }
}

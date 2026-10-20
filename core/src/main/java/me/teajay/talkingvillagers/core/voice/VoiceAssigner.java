package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.select.Selection;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;
import me.teajay.talkingvillagers.core.select.VoiceSelector;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Decides when an entity needs a (new) voice and which one it gets.
 */
public final class VoiceAssigner {
    private VoiceAssigner() {
    }

    /**
     * @param kind   what the entity is now; differs from the voice's entities after a villager turned into a witch
     * @param child  whether the entity is a child now
     * @param lookup finds an installed voice
     */
    public static boolean needsAssignment(Optional<VoiceIdentity> current, EntityKind kind, boolean child, Function<VoiceRef, Optional<ResolvedVoice>> lookup) {
        if (current.isPresent() && current.get().muted()) {
            return false;
        }
        if (current.isEmpty() || current.get().voice().isEmpty()) {
            return true;
        }
        VoiceIdentity identity = current.get();
        Optional<ResolvedVoice> voice = lookup.apply(identity.voice().get());
        if (voice.isEmpty()) {
            // the pack was removed
            return true;
        }
        if (!identity.manual() && !voice.get().voice().entities().contains(kind)) {
            // the voice came along when the entity was converted, e.g. from a villager to a witch; voices an admin
            // picked are kept, they may be meant for another kind on purpose
            return true;
        }
        // children get a new voice when they grow up, unless an admin picked the voice
        return !identity.manual() && identity.child() != child;
    }

    /**
     * Gives the entity a voice that fits its gender, age and language. Gender and language of an existing identity
     * are kept, so a child keeps both when it grows up.
     *
     * @param usage          how often each voice is used nearby, to give neighbours different voices
     * @param wantedLanguage language for an entity that has none yet, e.g. the language of its village
     * @return the new identity; without a fitting voice its {@link VoiceIdentity#voice()} is empty
     */
    public static VoiceIdentity assign(Optional<VoiceIdentity> current, EntityKind kind, boolean child, Collection<ResolvedVoice> pool,
                                       Map<VoiceRef, Integer> usage, AssignmentSettings settings, Optional<String> wantedLanguage,
                                       RandomGenerator random) {
        Gender gender = current.map(VoiceIdentity::gender).orElseGet(() -> rollGender(settings, kind, random));
        Optional<String> language = current.flatMap(VoiceIdentity::language).or(() -> wantedLanguage);

        VoiceCriteria criteria = VoiceCriteria.of(kind, child)
                .withGender(gender)
                .withChildFallback(settings.childFallback())
                .withUsage(usage)
                .avoiding(current.flatMap(VoiceIdentity::voice).map(Set::of).orElse(Set.of()));
        if (language.isPresent()) {
            criteria = criteria.withLanguage(language.get(), settings.strictLanguage());
        }

        Optional<Selection> selection = VoiceSelector.select(criteria, pool, random);
        if (selection.isEmpty()) {
            return VoiceIdentity.withoutVoice(gender, child, language);
        }
        ResolvedVoice voice = selection.get().voice();
        // remember the language of the first voice, so the entity keeps speaking it after growing up
        Optional<String> keptLanguage = language.isPresent() ? language : Optional.of(voice.language());
        return new VoiceIdentity(gender, Optional.of(voice.ref()), selection.get().samplePitch(random), child, keptLanguage, false, false);
    }

    public static Gender rollGender(AssignmentSettings settings, EntityKind kind, RandomGenerator random) {
        return random.nextDouble() < settings.femaleChance(kind) ? Gender.FEMALE : Gender.MALE;
    }
}

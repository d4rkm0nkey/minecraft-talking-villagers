package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static me.teajay.talkingvillagers.core.catalog.EntityKind.VILLAGER;
import static me.teajay.talkingvillagers.core.catalog.EntityKind.WITCH;
import static org.assertj.core.api.Assertions.assertThat;

class VoiceSelectorTest {
    private final RandomGenerator random = new SplittableRandom(42);

    static ResolvedVoice voice(String id, Set<EntityKind> kinds, Gender gender, boolean child, String language, int weight) {
        Voice voice = new Voice(id, id, kinds, gender, child, Optional.empty(), PitchRange.DEFAULT, weight, Map.of());
        return new ResolvedVoice(new VoiceRef("pack", id), voice, language);
    }

    static ResolvedVoice voice(String id, Gender gender, boolean child, String language) {
        return voice(id, Set.of(VILLAGER), gender, child, language, 1);
    }

    private Set<String> selectMany(VoiceCriteria criteria, List<ResolvedVoice> pool) {
        Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            VoiceSelector.select(criteria, pool, random).ifPresent(selection -> ids.add(selection.voice().ref().voiceId()));
        }
        return ids;
    }

    @Test
    void onlyPicksVoicesForTheEntityKind() {
        List<ResolvedVoice> pool = List.of(
                voice("villager", Set.of(VILLAGER), Gender.MALE, false, "de", 1),
                voice("witch", Set.of(WITCH), Gender.FEMALE, false, "de", 1));

        assertThat(selectMany(VoiceCriteria.of(WITCH, false), pool)).containsExactly("witch");
        assertThat(VoiceSelector.select(VoiceCriteria.of(EntityKind.EVOKER, false), pool, random)).isEmpty();
    }

    @Test
    void childrenOnlyGetChildVoicesAndAdultsOnlyAdultVoices() {
        List<ResolvedVoice> pool = List.of(voice("adult", Gender.MALE, false, "de"), voice("kid", Gender.MALE, true, "de"));

        assertThat(selectMany(VoiceCriteria.of(VILLAGER, true), pool)).containsExactly("kid");
        assertThat(selectMany(VoiceCriteria.of(VILLAGER, false), pool)).containsExactly("adult");
    }

    @Test
    void childWithoutChildVoiceKeepsVanillaByDefault() {
        List<ResolvedVoice> pool = List.of(voice("adult", Gender.MALE, false, "de"));

        assertThat(VoiceSelector.select(VoiceCriteria.of(VILLAGER, true), pool, random)).isEmpty();
    }

    @Test
    void childCanFallBackToPitchedUpAdultVoice() {
        List<ResolvedVoice> pool = List.of(voice("adult", Gender.MALE, false, "de"));
        VoiceCriteria criteria = VoiceCriteria.of(VILLAGER, true).withChildFallback(VoiceCriteria.ChildFallback.PITCH_UP_ADULT);

        Selection selection = VoiceSelector.select(criteria, pool, random).orElseThrow();

        assertThat(selection.adultForChild()).isTrue();
        assertThat(selection.samplePitch(random)).isGreaterThanOrEqualTo(PitchRange.DEFAULT.min() + Selection.CHILD_PITCH_BOOST);
    }

    @Test
    void prefersMatchingGenderAndAcceptsNeutralVoices() {
        List<ResolvedVoice> pool = List.of(
                voice("male", Gender.MALE, false, "de"),
                voice("female", Gender.FEMALE, false, "de"),
                voice("neutral", Gender.NEUTRAL, false, "de"));

        assertThat(selectMany(VoiceCriteria.of(VILLAGER, false).withGender(Gender.FEMALE), pool)).containsExactlyInAnyOrder("female", "neutral");
    }

    @Test
    void relaxesGenderWhenNothingElseFits() {
        List<ResolvedVoice> pool = List.of(voice("male", Gender.MALE, false, "de"));

        Selection selection = VoiceSelector.select(VoiceCriteria.of(VILLAGER, false).withGender(Gender.FEMALE), pool, random).orElseThrow();

        assertThat(selection.voice().ref().voiceId()).isEqualTo("male");
        assertThat(selection.genderRelaxed()).isTrue();
        assertThat(selection.languageRelaxed()).isFalse();
    }

    @Test
    void prefersLanguageAndMatchesByPrimaryLanguage() {
        List<ResolvedVoice> pool = List.of(voice("german", Gender.MALE, false, "de"), voice("british", Gender.MALE, false, "en-GB"));

        assertThat(selectMany(VoiceCriteria.of(VILLAGER, false).withLanguage("en", false), pool)).containsExactly("british");
    }

    @Test
    void relaxesLanguageBeforeGender() {
        List<ResolvedVoice> pool = List.of(voice("german-male", Gender.MALE, false, "de"), voice("english-female", Gender.FEMALE, false, "en"));
        VoiceCriteria criteria = VoiceCriteria.of(VILLAGER, false).withGender(Gender.FEMALE).withLanguage("de", false);

        Selection selection = VoiceSelector.select(criteria, pool, random).orElseThrow();

        assertThat(selection.voice().ref().voiceId()).isEqualTo("english-female");
        assertThat(selection.languageRelaxed()).isTrue();
        assertThat(selection.genderRelaxed()).isFalse();
    }

    @Test
    void strictLanguageNeverPicksAnotherLanguage() {
        List<ResolvedVoice> pool = List.of(voice("german-male", Gender.MALE, false, "de"), voice("english-female", Gender.FEMALE, false, "en"));
        VoiceCriteria criteria = VoiceCriteria.of(VILLAGER, false).withGender(Gender.FEMALE).withLanguage("de", true);

        Selection selection = VoiceSelector.select(criteria, pool, random).orElseThrow();
        assertThat(selection.voice().ref().voiceId()).isEqualTo("german-male");
        assertThat(selection.genderRelaxed()).isTrue();

        assertThat(VoiceSelector.select(VoiceCriteria.of(VILLAGER, false).withLanguage("fr", true), pool, random)).isEmpty();
    }

    @Test
    void avoidsVoicesOnlyIfAnotherFits() {
        ResolvedVoice a = voice("a", Gender.MALE, false, "de");
        ResolvedVoice b = voice("b", Gender.MALE, false, "de");

        assertThat(selectMany(VoiceCriteria.of(VILLAGER, false).avoiding(Set.of(a.ref())), List.of(a, b))).containsExactly("b");
        assertThat(selectMany(VoiceCriteria.of(VILLAGER, false).avoiding(Set.of(a.ref())), List.of(a))).containsExactly("a");
    }

    @Test
    void prefersLeastUsedVoices() {
        ResolvedVoice a = voice("a", Gender.MALE, false, "de");
        ResolvedVoice b = voice("b", Gender.MALE, false, "de");
        ResolvedVoice c = voice("c", Gender.MALE, false, "de");

        VoiceCriteria criteria = VoiceCriteria.of(VILLAGER, false).withUsage(Map.of(a.ref(), 3, b.ref(), 1));

        assertThat(selectMany(criteria, List.of(a, b, c))).containsExactly("c");
    }

    @Test
    void respectsWeights() {
        ResolvedVoice rare = voice("rare", Set.of(VILLAGER), Gender.MALE, false, "de", 1);
        ResolvedVoice common = voice("common", Set.of(VILLAGER), Gender.MALE, false, "de", 9);
        Map<String, Integer> counts = new HashMap<>();

        for (int i = 0; i < 10_000; i++) {
            String id = VoiceSelector.select(VoiceCriteria.of(VILLAGER, false), List.of(rare, common), random).orElseThrow().voice().ref().voiceId();
            counts.merge(id, 1, Integer::sum);
        }

        assertThat(counts.get("common")).isBetween(8_700, 9_300);
    }
}

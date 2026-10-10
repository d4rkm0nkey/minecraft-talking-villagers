package me.teajay.talkingvillagers.core.language;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {
    private final RandomGenerator random = new SplittableRandom(5);

    private static ResolvedVoice voice(String id, boolean child, String language) {
        Voice voice = new Voice(id, id, Set.of(EntityKind.VILLAGER), Gender.MALE, child, Optional.empty(), PitchRange.DEFAULT, 1, Map.of());
        return new ResolvedVoice(new VoiceRef("pack", id), voice, language);
    }

    private static final List<ResolvedVoice> VOICES = List.of(
            voice("a", false, "de"), voice("b", false, "en-GB"), voice("c", false, "fr"), voice("kid", true, "es"));

    @Test
    void candidatesAreAdultVoiceLanguages() {
        assertThat(LanguageChooser.candidates(VOICES, List.of())).containsExactly("de", "en-GB", "fr");
    }

    @Test
    void allowedLanguagesRestrictCandidates() {
        assertThat(LanguageChooser.candidates(VOICES, List.of("en", "de"))).containsExactly("de", "en-GB");
    }

    @Test
    void picksRandomlyAndCanAvoidALanguage() {
        Set<String> picked = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            picked.add(LanguageChooser.random(List.of("de", "en", "fr"), random).orElseThrow());
            assertThat(LanguageChooser.randomExcept(List.of("de", "en"), "de", random)).contains("en");
        }
        assertThat(picked).containsExactlyInAnyOrder("de", "en", "fr");
        assertThat(LanguageChooser.randomExcept(List.of("de"), "de", random)).contains("de");
        assertThat(LanguageChooser.random(List.of(), random)).isEmpty();
    }

    @Test
    void preferredTakesFirstAvailable() {
        List<String> candidates = List.of("de", "en-GB");

        assertThat(LanguageChooser.preferred(candidates, List.of("fr", "en", "de"))).contains("en-GB");
        assertThat(LanguageChooser.preferred(candidates, List.of("fr"))).isEmpty();
    }

    @Test
    void anchorsShareLanguageWithinRadius() {
        LanguageAnchors anchors = new LanguageAnchors();
        anchors.add(0, 64, 0, "de");
        anchors.add(200, 64, 0, "en");

        assertThat(anchors.find(30, 70, 30, 64)).map(LanguageAnchors.Anchor::language).contains("de");
        assertThat(anchors.find(180, 64, 0, 64)).map(LanguageAnchors.Anchor::language).contains("en");
        assertThat(anchors.find(100, 64, 0, 64)).isEmpty();
    }

    @Test
    void picksNearestAnchor() {
        LanguageAnchors anchors = new LanguageAnchors(List.of(new LanguageAnchors.Anchor(0, 0, 0, "de"), new LanguageAnchors.Anchor(50, 0, 0, "en")));

        assertThat(anchors.find(30, 0, 0, 64)).map(LanguageAnchors.Anchor::language).contains("en");
    }

    @Test
    void setLanguageChangesAnchorsInRangeOrCreatesOne() {
        LanguageAnchors anchors = new LanguageAnchors();
        anchors.add(0, 0, 0, "de");
        anchors.add(10, 0, 0, "de");

        assertThat(anchors.setLanguage(5, 0, 0, 64, "fr")).isEqualTo(2);
        assertThat(anchors.anchors()).extracting(LanguageAnchors.Anchor::language).containsOnly("fr");

        assertThat(anchors.setLanguage(500, 0, 0, 64, "en")).isEqualTo(1);
        assertThat(anchors.find(500, 0, 0, 1)).map(LanguageAnchors.Anchor::language).contains("en");
    }

    @Test
    void modesHaveIds() {
        assertThat(LanguageMode.byId("per_village")).contains(LanguageMode.PER_VILLAGE);
        assertThat(LanguageMode.byId("nope")).isEmpty();
    }
}

package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.select.Selection;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static me.teajay.talkingvillagers.core.catalog.EntityKind.VILLAGER;
import static org.assertj.core.api.Assertions.assertThat;

class VoiceAssignerTest {
    private final RandomGenerator random = new SplittableRandom(3);

    private static ResolvedVoice voice(String id, Gender gender, boolean child, String language) {
        Voice voice = new Voice(id, id, Set.of(VILLAGER), gender, child, Optional.empty(), new PitchRange(1.0f, 1.0f), 1, Map.of());
        return new ResolvedVoice(new VoiceRef("pack", id), voice, language);
    }

    private static final ResolvedVoice MAN = voice("man", Gender.MALE, false, "de");
    private static final ResolvedVoice WOMAN = voice("woman", Gender.FEMALE, false, "de");
    private static final ResolvedVoice BOY = voice("boy", Gender.MALE, true, "de");
    private static final ResolvedVoice GIRL = voice("girl", Gender.FEMALE, true, "de");
    private static final ResolvedVoice ENGLISH_MAN = voice("english-man", Gender.MALE, false, "en");
    private static final List<ResolvedVoice> POOL = List.of(MAN, WOMAN, BOY, GIRL, ENGLISH_MAN);

    private VoiceIdentity assign(Optional<VoiceIdentity> current, boolean child, List<ResolvedVoice> pool) {
        return VoiceAssigner.assign(current, VILLAGER, child, pool, Map.of(), AssignmentSettings.DEFAULT, random);
    }

    private static Optional<ResolvedVoice> lookup(VoiceRef ref) {
        return POOL.stream().filter(voice -> voice.ref().equals(ref)).findFirst();
    }

    @Test
    void newChildGetsChildVoiceMatchingItsGender() {
        for (int i = 0; i < 50; i++) {
            VoiceIdentity identity = assign(Optional.empty(), true, POOL);
            assertThat(identity.child()).isTrue();
            String expected = identity.gender() == Gender.MALE ? "boy" : "girl";
            assertThat(identity.voice()).contains(new VoiceRef("pack", expected));
        }
    }

    @Test
    void rollsBothGenders() {
        Set<Gender> genders = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            genders.add(VoiceAssigner.rollGender(AssignmentSettings.DEFAULT, random));
        }
        assertThat(genders).containsExactlyInAnyOrder(Gender.MALE, Gender.FEMALE);
        assertThat(VoiceAssigner.rollGender(new AssignmentSettings(1.0, VoiceCriteria.ChildFallback.VANILLA, false), random)).isEqualTo(Gender.FEMALE);
    }

    @Test
    void growingUpKeepsGenderAndLanguage() {
        VoiceIdentity girl = new VoiceIdentity(Gender.FEMALE, Optional.of(GIRL.ref()), 1.0f, true, Optional.of("de"), false);

        assertThat(VoiceAssigner.needsAssignment(Optional.of(girl), false, VoiceAssignerTest::lookup)).isTrue();
        VoiceIdentity adult = assign(Optional.of(girl), false, POOL);

        assertThat(adult.voice()).contains(WOMAN.ref());
        assertThat(adult.gender()).isEqualTo(Gender.FEMALE);
        assertThat(adult.child()).isFalse();
        assertThat(adult.language()).contains("de");
    }

    @Test
    void keepsLanguageOverOtherVoices() {
        VoiceIdentity englishBoy = new VoiceIdentity(Gender.MALE, Optional.empty(), 1.0f, true, Optional.of("en"), false);

        for (int i = 0; i < 20; i++) {
            assertThat(assign(Optional.of(englishBoy), false, POOL).voice()).contains(ENGLISH_MAN.ref());
        }
    }

    @Test
    void remembersLanguageOfFirstVoice() {
        VoiceIdentity identity = assign(Optional.of(new VoiceIdentity(Gender.MALE, Optional.empty(), 1, false, Optional.empty(), false)), false, List.of(ENGLISH_MAN));

        assertThat(identity.language()).contains("en");
    }

    @Test
    void manualVoiceSurvivesGrowingUp() {
        VoiceIdentity manual = new VoiceIdentity(Gender.MALE, Optional.of(MAN.ref()), 1.0f, true, Optional.empty(), true);

        assertThat(VoiceAssigner.needsAssignment(Optional.of(manual), false, VoiceAssignerTest::lookup)).isFalse();
    }

    @Test
    void reassignsWhenPackWasRemoved() {
        VoiceIdentity gone = new VoiceIdentity(Gender.MALE, Optional.of(new VoiceRef("removed", "x")), 1.0f, false, Optional.empty(), true);
        VoiceIdentity fine = new VoiceIdentity(Gender.MALE, Optional.of(MAN.ref()), 1.0f, false, Optional.empty(), false);

        assertThat(VoiceAssigner.needsAssignment(Optional.of(gone), false, VoiceAssignerTest::lookup)).isTrue();
        assertThat(VoiceAssigner.needsAssignment(Optional.of(fine), false, VoiceAssignerTest::lookup)).isFalse();
        assertThat(VoiceAssigner.needsAssignment(Optional.empty(), false, VoiceAssignerTest::lookup)).isTrue();
    }

    @Test
    void withoutFittingVoiceKeepsGenderAndHasNoVoice() {
        VoiceIdentity identity = assign(Optional.empty(), true, List.of(MAN, WOMAN));

        assertThat(identity.voice()).isEmpty();
        assertThat(identity.child()).isTrue();
        assertThat(VoiceAssigner.assign(Optional.of(identity), VILLAGER, true, List.of(), Map.of(), AssignmentSettings.DEFAULT, random).gender())
                .isEqualTo(identity.gender());
    }

    @Test
    void childFallbackPitchesAdultVoiceUp() {
        AssignmentSettings settings = new AssignmentSettings(0.0, VoiceCriteria.ChildFallback.PITCH_UP_ADULT, false);

        VoiceIdentity identity = VoiceAssigner.assign(Optional.empty(), VILLAGER, true, List.of(MAN), Map.of(), settings, random);

        assertThat(identity.voice()).contains(MAN.ref());
        assertThat(identity.pitch()).isEqualTo(1.0f + Selection.CHILD_PITCH_BOOST);
    }
}

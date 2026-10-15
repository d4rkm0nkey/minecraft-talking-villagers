package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.Weather;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class LineSelectorTest {
    private final RandomGenerator random = new SplittableRandom(7);

    private static VoiceLine line(String file, Set<String> professions, Weather weather) {
        return new VoiceLine(file, Optional.empty(), Optional.empty(), new LineConditions(professions, Optional.ofNullable(weather)));
    }

    private static Voice voice(Map<String, List<VoiceLine>> lines) {
        return new Voice("v", "V", Set.of(EntityKind.VILLAGER, EntityKind.EVOKER), Gender.MALE, false, Optional.empty(), PitchRange.DEFAULT, 1, lines);
    }

    private Set<String> pickMany(Voice voice, LineContext context) {
        Set<String> files = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            LineSelector.pick(voice, EventCatalog.RANDOM, context, OptionalInt.empty(), random).ifPresent(picked -> files.add(picked.line().file()));
        }
        return files;
    }

    @Test
    void mixesProfessionLinesWithGeneralLines() {
        Voice voice = voice(Map.of("random", List.of(
                VoiceLine.of("general"),
                line("farmer", Set.of("farmer"), null),
                line("cleric", Set.of("cleric"), null))));

        assertThat(pickMany(voice, new LineContext(Optional.of("minecraft:farmer"), Optional.empty()))).containsExactlyInAnyOrder("general", "farmer");
        assertThat(pickMany(voice, LineContext.NONE)).containsExactly("general");
    }

    @Test
    void usesSpecificLinesWhenThereAreNoGeneralOnes() {
        Voice voice = voice(Map.of("random", List.of(line("rain", Set.of(), Weather.RAIN))));

        assertThat(pickMany(voice, new LineContext(Optional.empty(), Optional.of(Weather.RAIN)))).containsExactly("rain");
        assertThat(pickMany(voice, new LineContext(Optional.empty(), Optional.of(Weather.CLEAR)))).isEmpty();
    }

    @Test
    void doesNotRepeatTheLastLine() {
        Voice voice = voice(Map.of("random", List.of(VoiceLine.of("a"), VoiceLine.of("b"))));

        for (int i = 0; i < 50; i++) {
            PickedLine picked = LineSelector.pick(voice, EventCatalog.RANDOM, LineContext.NONE, OptionalInt.of(0), random).orElseThrow();
            assertThat(picked.index()).isEqualTo(1);
        }
    }

    @Test
    void repeatsTheOnlyLine() {
        Voice voice = voice(Map.of("random", List.of(VoiceLine.of("a"))));

        assertThat(LineSelector.pick(voice, EventCatalog.RANDOM, LineContext.NONE, OptionalInt.of(0), random)).isPresent();
    }

    @Test
    void fallsBackToParentEvent() {
        Voice voice = voice(Map.of("cast_spell", List.of(VoiceLine.of("spell"))));

        PickedLine picked = LineSelector.pick(voice, EventCatalog.SUMMON_VEX, LineContext.NONE, OptionalInt.empty(), random).orElseThrow();

        assertThat(picked.event()).isEqualTo(EventCatalog.CAST_SPELL);
        assertThat(picked.line().file()).isEqualTo("spell");
    }

    @Test
    void returnsNothingWithoutLines() {
        assertThat(LineSelector.pick(voice(Map.of()), EventCatalog.HURT, LineContext.NONE, OptionalInt.empty(), random)).isEmpty();
    }
}

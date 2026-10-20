package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.select.LineContext;
import me.teajay.talkingvillagers.core.select.LineSelector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class HostileChatterTest {
    private final RandomGenerator random = new SplittableRandom(13);

    private static HostileChatter.Situation situation(long time, boolean fighting) {
        return new HostileChatter.Situation(time, fighting, fighting ? OptionalDouble.of(12) : OptionalDouble.empty(), false, false, false, 1.0f, false);
    }

    @Test
    void ambientDependsOnState() {
        HostileChatter.Situation idle = situation(0, false);
        assertThat(HostileChatter.ambientEvent(EntityKind.PILLAGER, idle)).isEqualTo(EventCatalog.IDLE);
        assertThat(HostileChatter.ambientEvent(EntityKind.WITCH, idle)).isEqualTo(EventCatalog.BREW);
        assertThat(HostileChatter.ambientEvent(EntityKind.PILLAGER, situation(0, true))).isEqualTo(EventCatalog.COMBAT);
        assertThat(HostileChatter.ambientEvent(EntityKind.PILLAGER, new HostileChatter.Situation(0, false, OptionalDouble.empty(), true, true, true, 1, false)))
                .isEqualTo(EventCatalog.RAID_IDLE);
        assertThat(HostileChatter.ambientEvent(EntityKind.PILLAGER, new HostileChatter.Situation(0, false, OptionalDouble.empty(), false, true, true, 1, false)))
                .isEqualTo(EventCatalog.PATROL_LEADER);
        assertThat(HostileChatter.ambientEvent(EntityKind.PILLAGER, new HostileChatter.Situation(0, false, OptionalDouble.empty(), false, true, false, 1, false)))
                .isEqualTo(EventCatalog.PATROL);
        assertThat(HostileChatter.ambientEvent(EntityKind.WITCH, new HostileChatter.Situation(0, false, OptionalDouble.empty(), false, true, false, 1, false)))
                .isEqualTo(EventCatalog.BREW);
        assertThat(HostileChatter.ambientEvent(EntityKind.VINDICATOR, new HostileChatter.Situation(0, true, OptionalDouble.of(2), false, false, false, 1, true)))
                .isEqualTo(EventCatalog.JOHNNY);
    }

    @Test
    void tauntsRegularlyDuringAFightAndCommentsWhenTargetIsLost() {
        SpeakerState state = new SpeakerState();
        List<VoiceEvent> said = new ArrayList<>();
        long time = 0;
        for (; time < 2400; time += Chatter.INTERVAL) {
            HostileChatter.tick(EntityKind.PILLAGER, state, situation(time, true), random).ifPresent(said::add);
        }
        // two minutes of fighting with a line every 8 to 15 seconds
        assertThat(said).containsOnly(EventCatalog.COMBAT).hasSizeBetween(8, 15);

        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, situation(time, false), random)).contains(EventCatalog.LOST_TARGET);
        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, situation(time + 20, false), random)).isEmpty();
    }

    @Test
    void noLostTargetAfterAVictory() {
        SpeakerState state = new SpeakerState();
        HostileChatter.tick(EntityKind.PILLAGER, state, situation(0, true), random);
        state.setLastVictory(40);

        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, situation(60, false), random)).isEmpty();
    }

    @Test
    void firstTauntComesAfterAPause() {
        SpeakerState state = new SpeakerState();

        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, situation(0, true), random)).isEmpty();
        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, situation(20, true), random)).isEmpty();
    }

    @Test
    void fightStartingAtLowHealthStillWaitsForTheFirstTaunt() {
        SpeakerState state = new SpeakerState();
        HostileChatter.Situation start = new HostileChatter.Situation(0, true, OptionalDouble.of(12), false, false, false, 0.2f, false);

        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state, start, random)).contains(EventCatalog.LOW_HEALTH);
        assertThat(state.inCombat()).isTrue();
        assertThat(HostileChatter.tick(EntityKind.PILLAGER, state,
                new HostileChatter.Situation(20, true, OptionalDouble.of(12), false, false, false, 0.2f, false), random)).isEmpty();
    }

    @Test
    void lowHealthIsSaidOnceUntilRecovered() {
        SpeakerState state = new SpeakerState();
        HostileChatter.Situation hurt = new HostileChatter.Situation(0, false, OptionalDouble.empty(), false, false, false, 0.2f, false);
        HostileChatter.Situation healed = new HostileChatter.Situation(0, false, OptionalDouble.empty(), false, false, false, 0.9f, false);

        assertThat(HostileChatter.tick(EntityKind.WITCH, state, hurt, random)).contains(EventCatalog.LOW_HEALTH);
        assertThat(HostileChatter.tick(EntityKind.WITCH, state, hurt, random)).isEmpty();
        HostileChatter.tick(EntityKind.WITCH, state, healed, random);
        assertThat(HostileChatter.tick(EntityKind.WITCH, state, hurt, random)).contains(EventCatalog.LOW_HEALTH);
    }

    @Test
    void evokerRetreatsFromClosePlayers() {
        SpeakerState state = new SpeakerState();
        HostileChatter.Situation close = new HostileChatter.Situation(0, true, OptionalDouble.of(4), false, false, false, 1, false);
        HostileChatter.tick(EntityKind.EVOKER, state, close, random);

        boolean retreated = false;
        for (long time = 20; time < 400; time += 20) {
            Optional<VoiceEvent> event = HostileChatter.tick(EntityKind.EVOKER, state,
                    new HostileChatter.Situation(time, true, OptionalDouble.of(4), false, false, false, 1, false), random);
            if (event.filter(EventCatalog.RETREAT::equals).isPresent()) {
                retreated = true;
                state.spoke(EventCatalog.RETREAT, "retreat", 0, time, 20);
            }
        }
        assertThat(retreated).isTrue();
    }

    @Test
    void stateEventsFallBackToAmbientForOlderPacks() {
        Voice voice = new Voice("v", "V", Set.of(EntityKind.PILLAGER), Gender.MALE, false, Optional.empty(), PitchRange.DEFAULT, 1,
                Map.of("ambient", List.of(VoiceLine.of("sounds/v/ambient/a.ogg"))));

        for (VoiceEvent event : List.of(EventCatalog.IDLE, EventCatalog.COMBAT, EventCatalog.RAID_IDLE, EventCatalog.PATROL_LEADER)) {
            assertThat(LineSelector.pick(voice, event, LineContext.NONE, id -> OptionalInt.empty(), random))
                    .as(event.id()).hasValueSatisfying(picked -> assertThat(picked.event()).isEqualTo(EventCatalog.AMBIENT));
        }
        assertThat(LineSelector.pick(voice, EventCatalog.WOLOLO, LineContext.NONE, id -> OptionalInt.empty(), random)).isEmpty();
    }
}

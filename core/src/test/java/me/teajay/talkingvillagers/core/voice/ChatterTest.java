package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Weather;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class ChatterTest {
    private final RandomGenerator random = new SplittableRandom(11);

    private Map<VoiceEvent, Integer> simulate(SpeakerState state, long ticks, OptionalDouble player, boolean hero, Weather weather, boolean day) {
        Map<VoiceEvent, Integer> counts = new HashMap<>();
        for (long time = 0; time < ticks; time += Chatter.INTERVAL) {
            Optional<VoiceEvent> event = Chatter.decide(state, Chatter.Situation.calm(time, player, hero, weather, day, true), random);
            if (event.isPresent()) {
                counts.merge(event.get(), 1, Integer::sum);
                state.spoke(event.get(), event.get().id(), 0, time, 40);
            }
        }
        return counts;
    }

    @Test
    void greetsCloseRarelyAndRespectsCooldown() {
        Map<VoiceEvent, Integer> counts = simulate(new SpeakerState(), 24_000, OptionalDouble.of(3), false, Weather.CLEAR, true);

        // greeting cooldown is 6000 ticks, so at most 4 greetings per Minecraft day
        assertThat(counts.get(EventCatalog.GREETING)).isBetween(3, 4);
    }

    @Test
    void doesNotGreetDistantPlayers() {
        Map<VoiceEvent, Integer> counts = simulate(new SpeakerState(), 24_000, OptionalDouble.of(10), false, Weather.CLEAR, true);

        assertThat(counts).doesNotContainKey(EventCatalog.GREETING);
    }

    @Test
    void talksToTheHero() {
        Map<VoiceEvent, Integer> counts = simulate(new SpeakerState(), 6_000, OptionalDouble.of(10), true, Weather.CLEAR, true);

        assertThat(counts.get(EventCatalog.HERO)).isBetween(2, 3);
    }

    @Test
    void smallTalkHappensEveryFewMinutesAndSometimesAboutWeather() {
        Map<VoiceEvent, Integer> counts = simulate(new SpeakerState(), 24_000 * 20, OptionalDouble.empty(), false, Weather.RAIN, true);

        // the delay between small talk is 300 to 5000 ticks
        int days = 20;
        assertThat(counts.get(EventCatalog.RANDOM) / days).isBetween(5, 16);
        assertThat(counts.get(EventCatalog.BAD_WEATHER)).isPositive();
        assertThat(counts).doesNotContainKey(EventCatalog.GOOD_WEATHER);
    }

    @Test
    void entitiesWithoutSmallTalkOnlyGreet() {
        SpeakerState state = new SpeakerState();
        Map<VoiceEvent, Integer> counts = new HashMap<>();
        for (long time = 0; time < 48_000; time += Chatter.INTERVAL) {
            Chatter.decide(state, Chatter.Situation.calm(time, OptionalDouble.of(2), false, Weather.RAIN, true, false), random)
                    .ifPresent(event -> {
                        counts.merge(event, 1, Integer::sum);
                        state.spoke(event, event.id(), 0, 0, 0);
                    });
        }

        assertThat(counts).containsOnlyKeys(EventCatalog.GREETING);
    }

    @Test
    void staysQuietWhileSpeaking() {
        SpeakerState state = new SpeakerState();
        state.spoke(EventCatalog.RANDOM, "random", 0, 0, 1000);

        for (long time = 0; time < 1000; time += Chatter.INTERVAL) {
            assertThat(Chatter.decide(state, Chatter.Situation.calm(time, OptionalDouble.of(1), true, Weather.CLEAR, true, true), random)).isEmpty();
        }
    }

    @Test
    void policiesHonourCooldownChanceAndInterrupt() {
        SpeakerState state = new SpeakerState();
        EventPolicy hurt = EventPolicies.of(EventCatalog.HURT);
        state.spoke(EventCatalog.RANDOM, "random", 0, 0, 100);

        // hurt interrupts a running line
        assertThat(state.mayVoice(EventCatalog.HURT, hurt, 50, random)).isTrue();
        // but a greeting does not
        assertThat(state.mayVoice(EventCatalog.YES, EventPolicies.of(EventCatalog.YES), 50, random)).isFalse();

        state.spoke(EventCatalog.HURT, "hurt", 0, 200, 5);
        assertThat(state.isReady(EventCatalog.HURT, hurt, 205)).isFalse();
        assertThat(state.isReady(EventCatalog.HURT, hurt, 210)).isTrue();
        assertThat(state.lastLine("hurt")).hasValue(0);
    }

    private static Chatter.Situation villager(long time, VillagerActivity activity, Threat threat, OptionalInt reputation) {
        return new Chatter.Situation(time, OptionalDouble.of(2), false, Weather.CLEAR, true, true, activity, threat, reputation);
    }

    @Test
    void panicStartsWithTheThreatAndKeepsCallingForHelp() {
        SpeakerState state = new SpeakerState();
        List<VoiceEvent> said = new ArrayList<>();
        for (long time = 0; time < 1200; time += Chatter.INTERVAL) {
            Chatter.decide(state, villager(time, VillagerActivity.PANIC, Threat.ZOMBIE, OptionalInt.empty()), random).ifPresent(said::add);
        }

        assertThat(said.getFirst()).isEqualTo(EventCatalog.SPOT_ZOMBIE);
        // a call every 5 to 8 seconds, and no greeting or small talk although a player is close
        assertThat(said.subList(1, said.size())).containsOnly(EventCatalog.PANIC_LOOP).hasSizeBetween(7, 12);
    }

    @Test
    void panicReasonDecidesFirstLine() {
        assertThat(Chatter.decide(new SpeakerState(), villager(0, VillagerActivity.PANIC, Threat.RAIDER, OptionalInt.empty()), random))
                .contains(EventCatalog.SPOT_RAIDER);
        assertThat(Chatter.decide(new SpeakerState(), villager(0, VillagerActivity.PANIC, Threat.PLAYER, OptionalInt.empty()), random))
                .contains(EventCatalog.PANIC);
    }

    @Test
    void raidStartsWithAlarmAndHidingVillagersWhisperRarely() {
        SpeakerState state = new SpeakerState();
        assertThat(Chatter.decide(state, villager(0, VillagerActivity.PRE_RAID, Threat.NONE, OptionalInt.empty()), random)).contains(EventCatalog.PRE_RAID);
        assertThat(Chatter.decide(state, villager(20, VillagerActivity.HIDE, Threat.NONE, OptionalInt.empty()), random)).contains(EventCatalog.HIDE);

        int whispers = 0;
        for (long time = 40; time < 2440; time += Chatter.INTERVAL) {
            if (Chatter.decide(state, villager(time, VillagerActivity.HIDE, Threat.NONE, OptionalInt.empty()), random).isPresent()) {
                whispers++;
            }
        }
        // two minutes hiding, one whisper every 15 to 25 seconds
        assertThat(whispers).isBetween(4, 8);
    }

    @Test
    void greetingDependsOnReputation() {
        assertThat(Chatter.greeting(OptionalInt.of(-50))).isEqualTo(EventCatalog.GREETING_UNFRIENDLY);
        assertThat(Chatter.greeting(OptionalInt.of(0))).isEqualTo(EventCatalog.GREETING);
        assertThat(Chatter.greeting(OptionalInt.empty())).isEqualTo(EventCatalog.GREETING);
        assertThat(Chatter.greeting(OptionalInt.of(80))).isEqualTo(EventCatalog.GREETING_FRIENDLY);
    }

    @Test
    void smallTalkFitsTheActivity() {
        Map<VoiceEvent, Integer> counts = new HashMap<>();
        SpeakerState state = new SpeakerState();
        for (long time = 0; time < 24_000 * 5; time += Chatter.INTERVAL) {
            Chatter.Situation working = new Chatter.Situation(time, OptionalDouble.empty(), false, Weather.CLEAR, true, true,
                    VillagerActivity.WORK, Threat.NONE, OptionalInt.empty());
            Chatter.decide(state, working, random).ifPresent(event -> counts.merge(event, 1, Integer::sum));
        }

        assertThat(counts).containsOnlyKeys(EventCatalog.WORK);
    }

    @Test
    void ambientTurnsIntoAlarmLines() {
        assertThat(Chatter.ambientEvent(VillagerActivity.PANIC)).isEqualTo(EventCatalog.PANIC_LOOP);
        assertThat(Chatter.ambientEvent(VillagerActivity.PRE_RAID)).isEqualTo(EventCatalog.PRE_RAID);
        assertThat(Chatter.ambientEvent(VillagerActivity.HIDE)).isEqualTo(EventCatalog.HIDE);
        assertThat(Chatter.ambientEvent(VillagerActivity.WORK)).isEqualTo(EventCatalog.AMBIENT);
    }
}

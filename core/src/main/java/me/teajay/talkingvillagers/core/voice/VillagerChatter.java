package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Weather;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.random.RandomGenerator;

/**
 * Decides when a villager says something on its own: greeting players, talking to the hero of the village and
 * random small talk. Evaluated once per {@link #INTERVAL} ticks.
 */
public final class VillagerChatter {
    public static final int INTERVAL = 20;
    /** Chance per check to start random small talk once the delay is over (V1: 1/150 per tick). */
    public static final double RANDOM_CHAT_CHANCE = 0.13;
    /** Share of random small talk that is about the weather. */
    public static final double WEATHER_CHAT_CHANCE = 0.1;

    private VillagerChatter() {
    }

    /**
     * @param nearestPlayerDistance distance to the nearest visible player, if any
     * @param heroNearby            a visible player has the hero of the village effect
     * @param daytime               whether it is day; small talk about good weather only happens by day
     */
    public record Situation(long gameTime, OptionalDouble nearestPlayerDistance, boolean heroNearby, Weather weather, boolean daytime) {
    }

    public static Optional<VoiceEvent> decide(SpeakerState state, Situation situation, RandomGenerator random) {
        long now = situation.gameTime();
        if (state.isSpeaking(now)) {
            return Optional.empty();
        }
        if (situation.heroNearby() && state.mayVoice(EventCatalog.HERO, EventPolicies.of(EventCatalog.HERO), now, random)) {
            return Optional.of(EventCatalog.HERO);
        }
        boolean playerClose = situation.nearestPlayerDistance().stream().anyMatch(distance -> distance <= EventPolicies.GREETING_DISTANCE);
        if (playerClose && state.mayVoice(EventCatalog.GREETING, EventPolicies.of(EventCatalog.GREETING), now, random)) {
            return Optional.of(EventCatalog.GREETING);
        }

        if (state.nextRandomChat() < 0) {
            scheduleRandomChat(state, now, random);
            return Optional.empty();
        }
        if (now < state.nextRandomChat() || random.nextDouble() >= RANDOM_CHAT_CHANCE) {
            return Optional.empty();
        }
        scheduleRandomChat(state, now, random);
        if (random.nextDouble() < WEATHER_CHAT_CHANCE) {
            if (situation.weather() != Weather.CLEAR) {
                return Optional.of(EventCatalog.BAD_WEATHER);
            }
            if (situation.daytime()) {
                return Optional.of(EventCatalog.GOOD_WEATHER);
            }
        }
        return Optional.of(EventCatalog.RANDOM);
    }

    private static void scheduleRandomChat(SpeakerState state, long now, RandomGenerator random) {
        state.setNextRandomChat(now + random.nextInt(EventPolicies.MIN_RANDOM_CHAT_DELAY, EventPolicies.MAX_RANDOM_CHAT_DELAY));
    }
}

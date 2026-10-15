package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Weather;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;

/**
 * Decides when a villager or wandering trader says something on its own. Evaluated once per {@link #INTERVAL} ticks.
 *
 * <p>In calm times villagers greet players (warmly or coldly, depending on reputation), talk to the hero of the
 * village and make small talk that fits what they are doing. When they panic, a raid starts or they hide, small talk
 * stops; instead they shout for help or whisper.
 */
public final class Chatter {
    public static final int INTERVAL = 20;
    /** Chance per check to start random small talk once the delay is over (V1: 1/150 per tick). */
    public static final double RANDOM_CHAT_CHANCE = 0.13;
    /** Share of random small talk that is about the weather. */
    public static final double WEATHER_CHAT_CHANCE = 0.1;

    private Chatter() {
    }

    /**
     * @param nearestPlayerDistance distance to the nearest visible player, if any
     * @param heroNearby            a visible player has the hero of the village effect
     * @param daytime               whether it is day; small talk about good weather only happens by day
     * @param smallTalk             whether the entity makes small talk on its own (villagers do, traders only greet)
     * @param activity              what the villager is doing; traders are always {@link VillagerActivity#IDLE}
     * @param threat                what the villager panics about
     * @param reputation            the nearest player's reputation with this villager
     */
    public record Situation(long gameTime, OptionalDouble nearestPlayerDistance, boolean heroNearby, Weather weather, boolean daytime,
                            boolean smallTalk, VillagerActivity activity, Threat threat, OptionalInt reputation) {
        public static Situation calm(long gameTime, OptionalDouble nearestPlayerDistance, boolean heroNearby, Weather weather, boolean daytime,
                                     boolean smallTalk) {
            return new Situation(gameTime, nearestPlayerDistance, heroNearby, weather, daytime, smallTalk, VillagerActivity.IDLE, Threat.NONE,
                    OptionalInt.empty());
        }
    }

    public static Optional<VoiceEvent> decide(SpeakerState state, Situation situation, RandomGenerator random) {
        long now = situation.gameTime();
        VillagerActivity previous = state.activity();
        state.setActivity(situation.activity());
        if (situation.activity().isAlarm()) {
            return alarm(state, situation, previous != situation.activity(), random);
        }

        if (state.isSpeaking(now)) {
            return Optional.empty();
        }
        if (situation.heroNearby() && state.mayVoice(EventCatalog.HERO, EventPolicies.of(EventCatalog.HERO), now, random)) {
            return Optional.of(EventCatalog.HERO);
        }
        boolean playerClose = situation.nearestPlayerDistance().stream().anyMatch(distance -> distance <= EventPolicies.GREETING_DISTANCE);
        VoiceEvent greeting = greeting(situation.reputation());
        if (playerClose && state.mayVoice(greeting, EventPolicies.of(greeting), now, random)) {
            return Optional.of(greeting);
        }

        if (!situation.smallTalk()) {
            return Optional.empty();
        }
        if (state.nextRandomChat() < 0) {
            scheduleRandomChat(state, now, random);
            return Optional.empty();
        }
        if (now < state.nextRandomChat() || random.nextDouble() >= RANDOM_CHAT_CHANCE) {
            return Optional.empty();
        }
        scheduleRandomChat(state, now, random);
        return Optional.of(smallTalk(situation, random));
    }

    /**
     * What a villager says instead of its vanilla ambient sound: in alarm states the ambient "hmm" turns into calls
     * for help or whispers.
     */
    public static VoiceEvent ambientEvent(VillagerActivity activity) {
        return switch (activity) {
            case PANIC -> EventCatalog.PANIC_LOOP;
            case PRE_RAID -> EventCatalog.PRE_RAID;
            case HIDE -> EventCatalog.HIDE;
            default -> EventCatalog.AMBIENT;
        };
    }

    public static VoiceEvent greeting(OptionalInt reputation) {
        if (reputation.isPresent() && reputation.getAsInt() <= EventPolicies.UNFRIENDLY_REPUTATION) {
            return EventCatalog.GREETING_UNFRIENDLY;
        }
        if (reputation.isPresent() && reputation.getAsInt() >= EventPolicies.FRIENDLY_REPUTATION) {
            return EventCatalog.GREETING_FRIENDLY;
        }
        return EventCatalog.GREETING;
    }

    private static VoiceEvent smallTalk(Situation situation, RandomGenerator random) {
        switch (situation.activity()) {
            case WORK:
                return EventCatalog.WORK;
            case PLAY:
                return EventCatalog.PLAY;
            case MEET:
                return EventCatalog.MEET;
            default:
                break;
        }
        if (random.nextDouble() < WEATHER_CHAT_CHANCE) {
            if (situation.weather() != Weather.CLEAR) {
                return EventCatalog.BAD_WEATHER;
            }
            if (situation.daytime()) {
                return EventCatalog.GOOD_WEATHER;
            }
        }
        return EventCatalog.RANDOM;
    }

    private static Optional<VoiceEvent> alarm(SpeakerState state, Situation situation, boolean started, RandomGenerator random) {
        long now = situation.gameTime();
        if (started) {
            scheduleAlarm(state, situation.activity(), now, random);
            return Optional.of(switch (situation.activity()) {
                case PANIC -> switch (situation.threat()) {
                    case ZOMBIE -> EventCatalog.SPOT_ZOMBIE;
                    case RAIDER -> EventCatalog.SPOT_RAIDER;
                    default -> EventCatalog.PANIC;
                };
                case PRE_RAID -> EventCatalog.PRE_RAID;
                default -> EventCatalog.HIDE;
            });
        }
        if (now < state.nextAlarmLine() || state.isSpeaking(now)) {
            return Optional.empty();
        }
        scheduleAlarm(state, situation.activity(), now, random);
        return Optional.of(ambientEvent(situation.activity()));
    }

    /** Panicking villagers call out every 5-8 s, villagers running home every 6-10 s, hiding ones whisper every 15-25 s. */
    private static void scheduleAlarm(SpeakerState state, VillagerActivity activity, long now, RandomGenerator random) {
        int delay = switch (activity) {
            case PANIC -> random.nextInt(100, 160);
            case PRE_RAID -> random.nextInt(120, 200);
            default -> random.nextInt(300, 500);
        };
        state.setNextAlarmLine(now + delay);
    }

    private static void scheduleRandomChat(SpeakerState state, long now, RandomGenerator random) {
        state.setNextRandomChat(now + random.nextInt(EventPolicies.MIN_RANDOM_CHAT_DELAY, EventPolicies.MAX_RANDOM_CHAT_DELAY));
    }
}

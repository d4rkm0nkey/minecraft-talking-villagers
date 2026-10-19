package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.random.RandomGenerator;

/**
 * States of witches and illagers. Their idle talk depends on whether they are on patrol, waiting in a raid or
 * fighting; on top of that they react when a fight ends, when they are badly hurt and taunt regularly in combat.
 *
 * <p>Idle talk follows the vanilla ambient timer: the ambient sound is replaced by {@link #ambientEvent}. Combat
 * taunts, lost targets and low health are checked every {@link Chatter#INTERVAL} ticks by {@link #tick}.
 */
public final class HostileChatter {
    public static final int MIN_COMBAT_DELAY = 160;
    public static final int MAX_COMBAT_DELAY = 300;
    /** Below this share of health the entity says its low health line once. */
    public static final float LOW_HEALTH = 0.3f;
    /** Above this share the low health line may be said again. */
    public static final float RECOVERED_HEALTH = 0.6f;
    /** The evoker backs off from players closer than this. */
    public static final double RETREAT_DISTANCE = 8.0;
    /** After a victory, losing the target is not worth a comment. */
    private static final int VICTORY_GRACE_TICKS = 100;

    private HostileChatter() {
    }

    /**
     * @param fighting       the entity has a target it fights, usually a player
     * @param targetDistance distance to that target
     * @param inRaid         the entity belongs to a running raid
     * @param johnny         a vindicator named Johnny, who attacks everything
     */
    public record Situation(long gameTime, boolean fighting, OptionalDouble targetDistance, boolean inRaid, boolean patrolling,
                            boolean patrolLeader, float healthFraction, boolean johnny) {
    }

    /**
     * What a hostile entity says instead of its vanilla ambient sound, depending on its state.
     */
    public static VoiceEvent ambientEvent(EntityKind kind, Situation situation) {
        if (situation.fighting()) {
            return situation.johnny() ? EventCatalog.JOHNNY : EventCatalog.COMBAT;
        }
        if (situation.inRaid()) {
            return EventCatalog.RAID_IDLE;
        }
        if (situation.patrolLeader()) {
            return EventCatalog.PATROL_LEADER;
        }
        if (situation.patrolling() && EntityKind.ILLAGERS.contains(kind)) {
            return EventCatalog.PATROL;
        }
        return kind == EntityKind.WITCH ? EventCatalog.BREW : EventCatalog.IDLE;
    }

    /**
     * Tracks the fight state and returns what the entity should say now, if anything. Starting a fight is voiced
     * elsewhere ({@code spot_player}), when the target is set.
     */
    public static Optional<VoiceEvent> tick(EntityKind kind, SpeakerState state, Situation situation, RandomGenerator random) {
        boolean lowHealth = false;
        if (situation.healthFraction() > RECOVERED_HEALTH) {
            state.setLowHealthSaid(false);
        } else if (situation.healthFraction() < LOW_HEALTH && !state.lowHealthSaid()) {
            state.setLowHealthSaid(true);
            lowHealth = true;
        }
        // the fight state is updated in any case, so a fight starting at low health still schedules its first taunt
        Optional<VoiceEvent> fight = fightEvent(kind, state, situation, random);
        return lowHealth ? Optional.of(EventCatalog.LOW_HEALTH) : fight;
    }

    private static Optional<VoiceEvent> fightEvent(EntityKind kind, SpeakerState state, Situation situation, RandomGenerator random) {
        long now = situation.gameTime();
        boolean wasFighting = state.inCombat();
        state.setInCombat(situation.fighting());

        if (wasFighting && !situation.fighting()) {
            boolean won = now - state.lastVictory() < VICTORY_GRACE_TICKS;
            return won ? Optional.empty() : Optional.of(EventCatalog.LOST_TARGET);
        }
        if (!situation.fighting()) {
            return Optional.empty();
        }
        if (!wasFighting) {
            // the fight just started; the first taunt comes after a pause
            scheduleCombatLine(state, now, random);
            return Optional.empty();
        }
        if (kind == EntityKind.EVOKER && situation.targetDistance().stream().anyMatch(distance -> distance < RETREAT_DISTANCE)
                && state.mayVoice(EventCatalog.RETREAT, EventPolicies.of(EventCatalog.RETREAT), now, random)) {
            return Optional.of(EventCatalog.RETREAT);
        }
        if (now >= state.nextCombatLine()) {
            scheduleCombatLine(state, now, random);
            return Optional.of(situation.johnny() ? EventCatalog.JOHNNY : EventCatalog.COMBAT);
        }
        return Optional.empty();
    }

    private static void scheduleCombatLine(SpeakerState state, long now, RandomGenerator random) {
        state.setNextCombatLine(now + random.nextInt(MIN_COMBAT_DELAY, MAX_COMBAT_DELAY));
    }
}

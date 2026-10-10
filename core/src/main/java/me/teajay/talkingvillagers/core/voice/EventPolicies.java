package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Default policies, mostly taken over from version 1.
 */
public final class EventPolicies {
    /** Ticks between ambient sounds of entities with a voice; vanilla villagers use 80. */
    public static final int AMBIENT_INTERVAL = 300;
    /** Radius for the limit of entities speaking at the same time (config {@code maxConcurrentSpeakers}). */
    public static final double CROWD_RADIUS = 32.0;
    /** Distance in blocks within which villagers greet players. */
    public static final double GREETING_DISTANCE = 5.0;
    public static final int MIN_RANDOM_CHAT_DELAY = 300;
    public static final int MAX_RANDOM_CHAT_DELAY = 5000;
    /** Reputation at or below which villagers greet a player coldly; iron golems attack at -100. */
    public static final int UNFRIENDLY_REPUTATION = -30;
    /** Reputation at or above which villagers greet a player warmly. */
    public static final int FRIENDLY_REPUTATION = 30;

    private static final Map<String, EventPolicy> POLICIES = new HashMap<>();

    static {
        put(EventCatalog.HURT, new EventPolicy(10, 1.0, true));
        put(EventCatalog.DEATH, new EventPolicy(0, 1.0, true));
        put(EventCatalog.GREETING, new EventPolicy(6000, 0.4, false));
        put(EventCatalog.GREETING_FRIENDLY, new EventPolicy(6000, 0.4, false));
        put(EventCatalog.GREETING_UNFRIENDLY, new EventPolicy(6000, 0.4, false));
        put(EventCatalog.ATTACKED_BY_PLAYER, new EventPolicy(60, 1.0, true));
        put(EventCatalog.SPOT_ZOMBIE, new EventPolicy(200, 1.0, true));
        put(EventCatalog.SPOT_RAIDER, new EventPolicy(200, 1.0, true));
        put(EventCatalog.PRE_RAID, new EventPolicy(0, 1.0, true));
        put(EventCatalog.VILLAGER_DIED, new EventPolicy(200, 0.8, true));
        put(EventCatalog.CURED, new EventPolicy(0, 1.0, true));
        put(EventCatalog.LOVE, new EventPolicy(200, 0.5, false));
        put(EventCatalog.HERO, new EventPolicy(2000, 0.5, false));
        put(EventCatalog.GOSSIP, new EventPolicy(1200, 0.5, false));
        put(EventCatalog.YES, new EventPolicy(20, 1.0, false));
        put(EventCatalog.NO, new EventPolicy(20, 1.0, false));
        put(EventCatalog.TRADE_SUCCESS, new EventPolicy(40, 1.0, false));
        put(EventCatalog.CELEBRATE, new EventPolicy(100, 1.0, false));
        put(EventCatalog.SLEEP, new EventPolicy(1200, 0.3, false));
        put(EventCatalog.WAKE_UP, new EventPolicy(1200, 0.3, false));
        put(EventCatalog.PANIC, new EventPolicy(200, 1.0, true));
        put(EventCatalog.GROW_UP, new EventPolicy(0, 1.0, true));
        put(EventCatalog.LEVEL_UP, new EventPolicy(0, 1.0, true));
        put(EventCatalog.DRINK_POTION, new EventPolicy(200, 1.0, false));
        put(EventCatalog.THROW_POTION, new EventPolicy(100, 0.4, false));
        put(EventCatalog.LAUGH, new EventPolicy(200, 0.7, true));
        put(EventCatalog.SPOT_PLAYER, new EventPolicy(400, 1.0, false));
        put(EventCatalog.ATTACK, new EventPolicy(100, 0.3, false));
        put(EventCatalog.CHARGE, new EventPolicy(200, 0.3, false));
        put(EventCatalog.CAST_SPELL, new EventPolicy(100, 1.0, false));
        put(EventCatalog.SUMMON_VEX, new EventPolicy(100, 1.0, false));
        put(EventCatalog.FANGS, new EventPolicy(100, 1.0, false));
        put(EventCatalog.MIRROR, new EventPolicy(100, 1.0, false));
        put(EventCatalog.RAID_HORN, new EventPolicy(0, 1.0, true));
        put(EventCatalog.COMBAT, new EventPolicy(HostileChatter.MIN_COMBAT_DELAY, 1.0, false));
        put(EventCatalog.JOHNNY, new EventPolicy(HostileChatter.MIN_COMBAT_DELAY, 1.0, false));
        put(EventCatalog.LOST_TARGET, new EventPolicy(600, 1.0, false));
        put(EventCatalog.LOW_HEALTH, new EventPolicy(1200, 1.0, false));
        put(EventCatalog.VICTORY, new EventPolicy(0, 1.0, true));
        put(EventCatalog.ALLY_DIED, new EventPolicy(200, 0.6, false));
        put(EventCatalog.CHARGE_AT, new EventPolicy(400, 1.0, false));
        put(EventCatalog.RETREAT, new EventPolicy(300, 0.7, false));
        put(EventCatalog.BLINDNESS, new EventPolicy(100, 1.0, false));
        put(EventCatalog.HEAL_SELF, new EventPolicy(200, 1.0, false));
        put(EventCatalog.DRINK_OTHER, new EventPolicy(200, 1.0, false));
        put(EventCatalog.HEAL_ALLY, new EventPolicy(200, 0.5, false));
    }

    private EventPolicies() {
    }

    private static void put(VoiceEvent event, EventPolicy policy) {
        POLICIES.put(event.id(), policy);
    }

    public static EventPolicy of(VoiceEvent event) {
        return POLICIES.getOrDefault(event.id(), EventPolicy.ALWAYS);
    }
}

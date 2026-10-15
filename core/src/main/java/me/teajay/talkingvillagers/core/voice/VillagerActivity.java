package me.teajay.talkingvillagers.core.voice;

/**
 * What a villager is doing, from its brain's active activity.
 */
public enum VillagerActivity {
    IDLE,
    WORK,
    PLAY,
    MEET,
    REST,
    /** Fleeing from a threat. */
    PANIC,
    /** The bell was rung, a raid is starting; villagers run home. */
    PRE_RAID,
    /** Hiding at home during a raid. */
    HIDE,
    CELEBRATE;

    public boolean isAlarm() {
        return this == PANIC || this == PRE_RAID || this == HIDE;
    }
}

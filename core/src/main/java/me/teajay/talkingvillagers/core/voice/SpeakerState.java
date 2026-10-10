package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.VoiceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;

/**
 * Runtime state of one speaking entity: cooldowns, the last line per event and chat timers. Not saved.
 */
public final class SpeakerState {
    private long busyUntil;
    private final Map<String, Long> lastSpoken = new HashMap<>();
    private final Map<String, Integer> lastLine = new HashMap<>();
    private long nextRandomChat = -1;
    private boolean panicking;
    private boolean inCombat;
    private boolean lowHealthSaid;
    private long nextCombatLine = -1;
    // far in the past, but without overflowing when the game time is subtracted
    private long lastVictory = Long.MIN_VALUE / 2;
    private Object failedAssignmentFor;

    public boolean isSpeaking(long gameTime) {
        return gameTime < busyUntil;
    }

    /**
     * Whether the event's cooldown is over. Does not roll the chance.
     */
    public boolean isReady(VoiceEvent event, EventPolicy policy, long gameTime) {
        Long last = lastSpoken.get(event.id());
        return last == null || gameTime - last >= policy.cooldownTicks();
    }

    /**
     * Cooldown, chance and whether the entity is free to speak.
     */
    public boolean mayVoice(VoiceEvent event, EventPolicy policy, long gameTime, RandomGenerator random) {
        if (!policy.interrupts() && isSpeaking(gameTime)) {
            return false;
        }
        return isReady(event, policy, gameTime) && random.nextDouble() < policy.chance();
    }

    public OptionalInt lastLine(String eventId) {
        Integer index = lastLine.get(eventId);
        return index == null ? OptionalInt.empty() : OptionalInt.of(index);
    }

    /**
     * @param requested the event that was asked for; its cooldown starts even when a parent event's line was used
     */
    public void spoke(VoiceEvent requested, String lineEventId, int lineIndex, long gameTime, int durationTicks) {
        busyUntil = gameTime + durationTicks;
        lastSpoken.put(requested.id(), gameTime);
        lastLine.put(lineEventId, lineIndex);
    }

    public long nextRandomChat() {
        return nextRandomChat;
    }

    public void setNextRandomChat(long gameTime) {
        this.nextRandomChat = gameTime;
    }

    /**
     * @return true if the entity just started panicking
     */
    public boolean updatePanic(boolean panicking) {
        boolean started = panicking && !this.panicking;
        this.panicking = panicking;
        return started;
    }

    public boolean inCombat() {
        return inCombat;
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public boolean lowHealthSaid() {
        return lowHealthSaid;
    }

    public void setLowHealthSaid(boolean said) {
        this.lowHealthSaid = said;
    }

    public long nextCombatLine() {
        return nextCombatLine;
    }

    public void setNextCombatLine(long gameTime) {
        this.nextCombatLine = gameTime;
    }

    public long lastVictory() {
        return lastVictory;
    }

    public void setLastVictory(long gameTime) {
        this.lastVictory = gameTime;
    }

    /**
     * Remembers that no voice was found for this pack snapshot, so the search is only repeated after packs change.
     */
    public boolean alreadyFailedFor(Object packSnapshot) {
        return failedAssignmentFor == packSnapshot;
    }

    public void assignmentFailedFor(Object packSnapshot) {
        this.failedAssignmentFor = packSnapshot;
    }
}

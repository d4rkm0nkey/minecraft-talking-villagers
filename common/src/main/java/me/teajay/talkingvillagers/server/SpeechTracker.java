package me.teajay.talkingvillagers.server;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.WeakHashMap;

/**
 * Remembers per entity until when it is speaking and which line it said last per event.
 * Server thread only; entries disappear with the entity objects.
 */
public final class SpeechTracker {
    private final Map<Entity, State> states = new WeakHashMap<>();

    private static final class State {
        long busyUntil;
        final Map<String, Integer> lastLine = new HashMap<>();
    }

    public boolean isSpeaking(Entity entity, long gameTime) {
        State state = states.get(entity);
        return state != null && gameTime < state.busyUntil;
    }

    public OptionalInt lastLine(Entity entity, String eventId) {
        State state = states.get(entity);
        Integer index = state == null ? null : state.lastLine.get(eventId);
        return index == null ? OptionalInt.empty() : OptionalInt.of(index);
    }

    public void spoke(Entity entity, String eventId, int lineIndex, long gameTime, int durationTicks) {
        State state = states.computeIfAbsent(entity, e -> new State());
        state.busyUntil = gameTime + durationTicks;
        state.lastLine.put(eventId, lineIndex);
    }
}

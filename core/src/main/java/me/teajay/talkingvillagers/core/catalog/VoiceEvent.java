package me.teajay.talkingvillagers.core.catalog;

import java.util.Optional;
import java.util.Set;

/**
 * A situation in which an entity can say something.
 *
 * @param id      manifest key, e.g. {@code greeting}
 * @param kinds   entity kinds that trigger this event
 * @param parent  event whose lines are used when a voice has no lines for this event
 */
public record VoiceEvent(String id, Set<EntityKind> kinds, Optional<VoiceEvent> parent) {
    public VoiceEvent {
        kinds = Set.copyOf(kinds);
    }

    public boolean appliesTo(EntityKind kind) {
        return kinds.contains(kind);
    }
}

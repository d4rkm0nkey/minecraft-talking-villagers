package me.teajay.talkingvillagers.core.model;

import java.util.Optional;
import java.util.Set;

/**
 * Restricts when a line may be spoken. Empty conditions match always.
 *
 * @param professions villager profession ids, e.g. {@code farmer}; empty means any
 */
public record LineConditions(Set<String> professions, Optional<Weather> weather) {
    public static final LineConditions NONE = new LineConditions(Set.of(), Optional.empty());

    public LineConditions {
        professions = Set.copyOf(professions);
    }

    public boolean isEmpty() {
        return professions.isEmpty() && weather.isEmpty();
    }
}

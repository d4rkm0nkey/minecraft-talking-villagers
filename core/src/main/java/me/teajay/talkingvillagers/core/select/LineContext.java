package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.Professions;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.Weather;

import java.util.Optional;

/**
 * The situation of the speaking entity, matched against {@link LineConditions}.
 *
 * @param profession villager profession id, empty for other entities
 */
public record LineContext(Optional<String> profession, Optional<Weather> weather) {
    public static final LineContext NONE = new LineContext(Optional.empty(), Optional.empty());

    public LineContext {
        profession = profession.map(Professions::normalize);
    }

    public boolean matches(LineConditions conditions) {
        if (!conditions.professions().isEmpty()
                && profession.map(id -> !conditions.professions().contains(id)).orElse(true)) {
            return false;
        }
        return conditions.weather().isEmpty() || conditions.weather().equals(weather);
    }
}

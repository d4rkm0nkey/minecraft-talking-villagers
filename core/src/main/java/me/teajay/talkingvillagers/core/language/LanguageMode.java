package me.teajay.talkingvillagers.core.language;

import java.util.Locale;
import java.util.Optional;

/**
 * How languages are chosen for entities.
 */
public enum LanguageMode {
    /** Language does not matter, every voice may be used. */
    ANY,
    /** Voices in the preferred languages are used where possible. */
    PREFERRED,
    /** Every village gets a random language that all its villagers speak. */
    PER_VILLAGE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<LanguageMode> byId(String id) {
        for (LanguageMode mode : values()) {
            if (mode.id().equals(id)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}

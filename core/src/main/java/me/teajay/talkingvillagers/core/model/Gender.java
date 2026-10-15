package me.teajay.talkingvillagers.core.model;

import java.util.Locale;
import java.util.Optional;

public enum Gender {
    MALE,
    FEMALE,
    /** Fits entities of any gender. */
    NEUTRAL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Gender> byId(String id) {
        for (Gender gender : values()) {
            if (gender.id().equals(id)) {
                return Optional.of(gender);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether a voice of this gender may be given to an entity of the wanted gender.
     */
    public boolean fits(Gender wanted) {
        return this == NEUTRAL || wanted == NEUTRAL || this == wanted;
    }
}

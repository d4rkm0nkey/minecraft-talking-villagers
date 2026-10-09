package me.teajay.talkingvillagers.core.model;

import java.util.Locale;
import java.util.Optional;

public enum Weather {
    CLEAR,
    RAIN,
    THUNDER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Weather> byId(String id) {
        for (Weather weather : values()) {
            if (weather.id().equals(id)) {
                return Optional.of(weather);
            }
        }
        return Optional.empty();
    }
}

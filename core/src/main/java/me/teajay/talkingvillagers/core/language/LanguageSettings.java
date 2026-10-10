package me.teajay.talkingvillagers.core.language;

import java.util.List;
import java.util.Locale;

/**
 * @param preferred     languages in order of preference, for {@link LanguageMode#PREFERRED}
 * @param allowed       languages that may be chosen at random; empty allows all installed languages
 * @param villageRadius distance in blocks within which bells and villagers belong to the same village
 */
public record LanguageSettings(LanguageMode mode, List<String> preferred, List<String> allowed, boolean strict, int villageRadius,
                               TraderLanguage traderLanguage, RaidLanguage raidLanguage) {
    public static final LanguageSettings DEFAULT = new LanguageSettings(LanguageMode.ANY, List.of(), List.of(), false, 64,
            TraderLanguage.RANDOM, RaidLanguage.RANDOM);

    public LanguageSettings {
        preferred = List.copyOf(preferred);
        allowed = List.copyOf(allowed);
    }

    public enum TraderLanguage {
        RANDOM,
        /** The language of the nearest village. */
        NEAREST_VILLAGE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum RaidLanguage {
        /** Every raid speaks a random language. */
        RANDOM,
        /** Raiders speak another language than the village they attack, if one is installed. */
        OPPOSITE_OF_VILLAGE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

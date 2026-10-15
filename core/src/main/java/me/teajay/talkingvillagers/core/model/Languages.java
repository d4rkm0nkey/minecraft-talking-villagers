package me.teajay.talkingvillagers.core.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers for BCP 47 language tags such as {@code de}, {@code en} or {@code en-GB}.
 */
public final class Languages {
    private static final Pattern TAG = Pattern.compile("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$");

    private Languages() {
    }

    public static boolean isValid(String tag) {
        return tag != null && TAG.matcher(tag).matches();
    }

    /**
     * Canonical form: lower case language, upper case two-letter region ({@code en-gb} becomes {@code en-GB}).
     */
    public static String normalize(String tag) {
        return Locale.forLanguageTag(tag).toLanguageTag();
    }

    public static String primary(String tag) {
        int dash = tag.indexOf('-');
        return (dash < 0 ? tag : tag.substring(0, dash)).toLowerCase(Locale.ROOT);
    }

    /**
     * Whether a voice speaking {@code voiceLanguage} is acceptable when {@code wanted} is requested.
     * Tags match exactly or by their primary language, so {@code en-GB} matches {@code en}.
     */
    public static boolean matches(String voiceLanguage, String wanted) {
        return voiceLanguage.equalsIgnoreCase(wanted) || primary(voiceLanguage).equals(primary(wanted));
    }
}

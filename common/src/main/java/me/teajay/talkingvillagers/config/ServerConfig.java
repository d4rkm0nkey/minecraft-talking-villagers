package me.teajay.talkingvillagers.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.teajay.talkingvillagers.core.language.LanguageMode;
import me.teajay.talkingvillagers.core.language.LanguageSettings;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;
import me.teajay.talkingvillagers.core.voice.AssignmentSettings;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Settings that decide which voices entities get. They apply on the server (or the integrated server in singleplayer).
 */
public record ServerConfig(
        double femaleChance,
        double witchFemaleChance,
        VoiceCriteria.ChildFallback childFallback,
        LanguageMode languageMode,
        List<String> preferredLanguages,
        List<String> allowedLanguages,
        boolean strictLanguage,
        int villageRadius,
        LanguageSettings.TraderLanguage traderLanguage,
        LanguageSettings.RaidLanguage raidLanguage
) {
    public static final ServerConfig DEFAULT = new ServerConfig(0.5, 1.0, VoiceCriteria.ChildFallback.VANILLA, LanguageMode.ANY,
            List.of(), List.of(), false, 64, LanguageSettings.TraderLanguage.RANDOM, LanguageSettings.RaidLanguage.RANDOM);

    public static final Codec<ServerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).optionalFieldOf("femaleChance", DEFAULT.femaleChance).forGetter(ServerConfig::femaleChance),
            Codec.doubleRange(0, 1).optionalFieldOf("witchFemaleChance", DEFAULT.witchFemaleChance).forGetter(ServerConfig::witchFemaleChance),
            enumCodec(VoiceCriteria.ChildFallback.values()).optionalFieldOf("childFallback", DEFAULT.childFallback).forGetter(ServerConfig::childFallback),
            enumCodec(LanguageMode.values()).optionalFieldOf("languageMode", DEFAULT.languageMode).forGetter(ServerConfig::languageMode),
            Codec.STRING.listOf().optionalFieldOf("preferredLanguages", DEFAULT.preferredLanguages).forGetter(ServerConfig::preferredLanguages),
            Codec.STRING.listOf().optionalFieldOf("allowedLanguages", DEFAULT.allowedLanguages).forGetter(ServerConfig::allowedLanguages),
            Codec.BOOL.optionalFieldOf("strictLanguage", DEFAULT.strictLanguage).forGetter(ServerConfig::strictLanguage),
            Codec.intRange(8, 512).optionalFieldOf("villageRadius", DEFAULT.villageRadius).forGetter(ServerConfig::villageRadius),
            enumCodec(LanguageSettings.TraderLanguage.values()).optionalFieldOf("traderLanguage", DEFAULT.traderLanguage).forGetter(ServerConfig::traderLanguage),
            enumCodec(LanguageSettings.RaidLanguage.values()).optionalFieldOf("raidLanguage", DEFAULT.raidLanguage).forGetter(ServerConfig::raidLanguage)
    ).apply(instance, ServerConfig::new));

    public AssignmentSettings assignment() {
        return new AssignmentSettings(femaleChance, witchFemaleChance, childFallback, strictLanguage);
    }

    public LanguageSettings language() {
        return new LanguageSettings(languageMode, preferredLanguages, allowedLanguages, strictLanguage, villageRadius, traderLanguage, raidLanguage);
    }

    /** Enums are written in lower case, e.g. {@code "per_village"}. */
    private static <E extends Enum<E>> Codec<E> enumCodec(E[] values) {
        Function<String, DataResult<E>> parse = id -> {
            for (E value : values) {
                if (value.name().toLowerCase(Locale.ROOT).equals(id)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> "Unknown value " + id);
        };
        return Codec.STRING.comapFlatMap(parse, value -> value.name().toLowerCase(Locale.ROOT));
    }
}

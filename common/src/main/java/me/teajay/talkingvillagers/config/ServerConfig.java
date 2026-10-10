package me.teajay.talkingvillagers.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.language.LanguageMode;
import me.teajay.talkingvillagers.core.language.LanguageSettings;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;
import me.teajay.talkingvillagers.core.voice.AssignmentSettings;

import java.util.LinkedHashSet;
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
        LanguageSettings.RaidLanguage raidLanguage,
        List<String> disabledEntities,
        List<String> disabledPacks,
        List<String> disabledVoices,
        int maxConcurrentSpeakers,
        PackDownload packDownload
) {
    /**
     * How players without the server's packs get them.
     *
     * @param url          if set, clients download {@code <url>/<file name>} over HTTP(S) instead of through the game
     * @param kbpsPerPlayer bandwidth per player when sending packs through the game connection
     */
    public record PackDownload(boolean enabled, String url, int kbpsPerPlayer, int maxPackSizeMb) {
        public static final PackDownload DEFAULT = new PackDownload(true, "", 2048, 100);
        public static final Codec<PackDownload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled).forGetter(PackDownload::enabled),
                Codec.STRING.optionalFieldOf("url", DEFAULT.url).forGetter(PackDownload::url),
                Codec.intRange(64, 1_000_000).optionalFieldOf("kbpsPerPlayer", DEFAULT.kbpsPerPlayer).forGetter(PackDownload::kbpsPerPlayer),
                Codec.intRange(1, 2000).optionalFieldOf("maxPackSizeMb", DEFAULT.maxPackSizeMb).forGetter(PackDownload::maxPackSizeMb)
        ).apply(instance, PackDownload::new));
    }

    public static final ServerConfig DEFAULT = new ServerConfig(0.5, 1.0, VoiceCriteria.ChildFallback.VANILLA, LanguageMode.ANY,
            List.of(), List.of(), false, 64, LanguageSettings.TraderLanguage.RANDOM, LanguageSettings.RaidLanguage.RANDOM,
            List.of(), List.of(), List.of(), 3, PackDownload.DEFAULT);

    public ServerConfig {
        preferredLanguages = List.copyOf(preferredLanguages);
        allowedLanguages = List.copyOf(allowedLanguages);
        disabledEntities = List.copyOf(disabledEntities);
        disabledPacks = List.copyOf(disabledPacks);
        disabledVoices = List.copyOf(disabledVoices);
    }

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
            enumCodec(LanguageSettings.RaidLanguage.values()).optionalFieldOf("raidLanguage", DEFAULT.raidLanguage).forGetter(ServerConfig::raidLanguage),
            Codec.STRING.listOf().optionalFieldOf("disabledEntities", DEFAULT.disabledEntities).forGetter(ServerConfig::disabledEntities),
            Codec.STRING.listOf().optionalFieldOf("disabledPacks", DEFAULT.disabledPacks).forGetter(ServerConfig::disabledPacks),
            Codec.STRING.listOf().optionalFieldOf("disabledVoices", DEFAULT.disabledVoices).forGetter(ServerConfig::disabledVoices),
            Codec.intRange(1, 50).optionalFieldOf("maxConcurrentSpeakers", DEFAULT.maxConcurrentSpeakers).forGetter(ServerConfig::maxConcurrentSpeakers),
            PackDownload.CODEC.optionalFieldOf("packDownload", PackDownload.DEFAULT).forGetter(ServerConfig::packDownload)
    ).apply(instance, ServerConfig::new));

    public boolean isEnabled(EntityKind kind) {
        return !disabledEntities.contains(kind.id());
    }

    public boolean isPackEnabled(String packId) {
        return !disabledPacks.contains(packId);
    }

    public boolean isVoiceEnabled(VoiceRef ref) {
        return !disabledVoices.contains(ref.toString());
    }

    // --- copies with one value changed, for the settings screen -------------------------------------------------------

    public ServerConfig withFemaleChance(double value) {
        return new ServerConfig(value, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withWitchFemaleChance(double value) {
        return new ServerConfig(femaleChance, value, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withChildFallback(VoiceCriteria.ChildFallback value) {
        return new ServerConfig(femaleChance, witchFemaleChance, value, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withLanguageMode(LanguageMode value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, value, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withPreferredLanguages(List<String> value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, value, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withAllowedLanguages(List<String> value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, value, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withStrictLanguage(boolean value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, value,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withTraderLanguage(LanguageSettings.TraderLanguage value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, value, raidLanguage, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withRaidLanguage(LanguageSettings.RaidLanguage value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, value, disabledEntities, disabledPacks, disabledVoices, maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withEntityEnabled(EntityKind kind, boolean enabled) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, toggle(disabledEntities, kind.id(), !enabled), disabledPacks, disabledVoices,
                maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withPackEnabled(String packId, boolean enabled) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, toggle(disabledPacks, packId, !enabled), disabledVoices,
                maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withVoiceEnabled(VoiceRef ref, boolean enabled) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, toggle(disabledVoices, ref.toString(), !enabled),
                maxConcurrentSpeakers, packDownload);
    }

    public ServerConfig withMaxConcurrentSpeakers(int value) {
        return new ServerConfig(femaleChance, witchFemaleChance, childFallback, languageMode, preferredLanguages, allowedLanguages, strictLanguage,
                villageRadius, traderLanguage, raidLanguage, disabledEntities, disabledPacks, disabledVoices, value, packDownload);
    }

    private static List<String> toggle(List<String> list, String value, boolean contained) {
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        if (contained) {
            set.add(value);
        } else {
            set.remove(value);
        }
        return List.copyOf(set);
    }

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

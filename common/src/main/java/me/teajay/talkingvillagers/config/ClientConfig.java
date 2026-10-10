package me.teajay.talkingvillagers.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/**
 * Settings that only affect what this player hears and sees.
 *
 * @param volume multiplier for all voice lines, on top of the vanilla sound categories
 */
public record ClientConfig(float volume, Subtitles subtitles) {
    public static final ClientConfig DEFAULT = new ClientConfig(1.0f, Subtitles.TRANSCRIPT);

    public enum Subtitles {
        /** The transcript of the line, or "Villager speaks" if the line has none. */
        TRANSCRIPT,
        /** Always "Villager speaks". */
        GENERIC,
        OFF;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final Codec<Subtitles> SUBTITLES = Codec.STRING.comapFlatMap(id -> {
        for (Subtitles value : Subtitles.values()) {
            if (value.id().equals(id)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() -> "Unknown subtitle mode " + id);
    }, Subtitles::id);

    public static final Codec<ClientConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0, 2).optionalFieldOf("volume", DEFAULT.volume).forGetter(ClientConfig::volume),
            SUBTITLES.optionalFieldOf("subtitles", DEFAULT.subtitles).forGetter(ClientConfig::subtitles)
    ).apply(instance, ClientConfig::new));

    public ClientConfig withVolume(float value) {
        return new ClientConfig(value, subtitles);
    }

    public ClientConfig withSubtitles(Subtitles value) {
        return new ClientConfig(volume, value);
    }
}

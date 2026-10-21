package me.teajay.talkingvillagers.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Settings that only affect what this player hears and sees.
 *
 * @param volume multiplier for all voice lines, on top of the vanilla sound categories
 */
public record ClientConfig(float volume, Subtitles subtitles, List<String> trustedServers) {
    public static final ClientConfig DEFAULT = new ClientConfig(1.0f, Subtitles.TRANSCRIPT, List.of());

    public ClientConfig {
        // Minecraft never plays louder than 100 %; more only made voices carry further. Older configs allowed up to 2.
        volume = Math.clamp(volume, 0.0f, 1.0f);
        trustedServers = List.copyOf(trustedServers);
    }

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
            Codec.FLOAT.optionalFieldOf("volume", DEFAULT.volume).forGetter(ClientConfig::volume),
            SUBTITLES.optionalFieldOf("subtitles", DEFAULT.subtitles).forGetter(ClientConfig::subtitles),
            Codec.STRING.listOf().optionalFieldOf("trustedServers", DEFAULT.trustedServers).forGetter(ClientConfig::trustedServers)
    ).apply(instance, ClientConfig::new));

    public ClientConfig withVolume(float value) {
        return new ClientConfig(value, subtitles, trustedServers);
    }

    public ClientConfig withSubtitles(Subtitles value) {
        return new ClientConfig(volume, value, trustedServers);
    }

    /** Servers whose voicepacks are downloaded without asking. */
    public ClientConfig trusting(String server) {
        List<String> servers = new ArrayList<>(trustedServers);
        if (!servers.contains(server)) {
            servers.add(server);
        }
        return new ClientConfig(volume, subtitles, servers);
    }

    public ClientConfig distrusting(String server) {
        List<String> servers = new ArrayList<>(trustedServers);
        servers.remove(server);
        return new ClientConfig(volume, subtitles, servers);
    }
}

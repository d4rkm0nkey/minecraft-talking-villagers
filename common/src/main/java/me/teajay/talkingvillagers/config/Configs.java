package me.teajay.talkingvillagers.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the config files. A missing file is created with the defaults; unknown or broken values fall back to defaults
 * and the file is rewritten, so it always lists every option.
 */
public final class Configs {
    private static final Path DIRECTORY = Services.PLATFORM.getGameDir().resolve("config");
    private static final Path SERVER_FILE = DIRECTORY.resolve(TalkingVillagers.MOD_ID + "-server.json");
    private static final Path CLIENT_FILE = DIRECTORY.resolve(TalkingVillagers.MOD_ID + "-client.json");
    private static volatile ServerConfig server = ServerConfig.DEFAULT;
    private static volatile ClientConfig client = ClientConfig.DEFAULT;

    private Configs() {
    }

    public static ServerConfig server() {
        return server;
    }

    public static ClientConfig client() {
        return client;
    }

    public static Path serverFile() {
        return SERVER_FILE;
    }

    public static void load() {
        server = read(SERVER_FILE, ServerConfig.CODEC, ServerConfig.DEFAULT);
        write(SERVER_FILE, ServerConfig.CODEC, server);
        applyPackFilter();
    }

    public static void loadClient() {
        client = read(CLIENT_FILE, ClientConfig.CODEC, ClientConfig.DEFAULT);
        write(CLIENT_FILE, ClientConfig.CODEC, client);
    }

    /**
     * Saves new server settings. Changing which packs are enabled reloads the packs.
     */
    public static void setServer(ServerConfig config) {
        boolean packsChanged = !config.disabledPacks().equals(server.disabledPacks());
        server = config;
        write(SERVER_FILE, ServerConfig.CODEC, config);
        applyPackFilter();
        if (packsChanged) {
            Voicepacks.reload();
        }
    }

    public static void setClient(ClientConfig config) {
        client = config;
        write(CLIENT_FILE, ClientConfig.CODEC, config);
    }

    private static void applyPackFilter() {
        Voicepacks.repository().setEnabled(packId -> server.isPackEnabled(packId));
    }

    private static <T> T read(Path file, Codec<T> codec, T defaults) {
        if (!Files.exists(file)) {
            return defaults;
        }
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            return codec.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> TalkingVillagers.LOG.warn("Problem in {}: {}", file, error))
                    .orElse(defaults);
        } catch (IOException | RuntimeException e) {
            TalkingVillagers.LOG.error("Cannot read {}, using defaults", file, e);
            return defaults;
        }
    }

    private static <T> void write(Path file, Codec<T> codec, T config) {
        try {
            Files.createDirectories(file.getParent());
            JsonElement json = codec.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (IOException | RuntimeException e) {
            TalkingVillagers.LOG.error("Cannot write {}", file, e);
        }
    }
}

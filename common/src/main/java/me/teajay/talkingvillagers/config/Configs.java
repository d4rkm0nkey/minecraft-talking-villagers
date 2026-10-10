package me.teajay.talkingvillagers.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the config files. A missing file is created with the defaults; unknown or broken values fall back to defaults
 * and the file is rewritten, so it always lists every option.
 */
public final class Configs {
    private static final Path SERVER_FILE = Services.PLATFORM.getGameDir().resolve("config").resolve(TalkingVillagers.MOD_ID + "-server.json");
    private static volatile ServerConfig server = ServerConfig.DEFAULT;

    private Configs() {
    }

    public static ServerConfig server() {
        return server;
    }

    public static Path serverFile() {
        return SERVER_FILE;
    }

    public static void load() {
        ServerConfig loaded = ServerConfig.DEFAULT;
        if (Files.exists(SERVER_FILE)) {
            try {
                JsonElement json = JsonParser.parseString(Files.readString(SERVER_FILE));
                loaded = ServerConfig.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> TalkingVillagers.LOG.warn("Problem in {}: {}", SERVER_FILE, error))
                        .orElse(ServerConfig.DEFAULT);
            } catch (IOException | RuntimeException e) {
                TalkingVillagers.LOG.error("Cannot read {}, using defaults", SERVER_FILE, e);
            }
        }
        server = loaded;
        save(loaded);
    }

    public static void setServer(ServerConfig config) {
        server = config;
        save(config);
    }

    private static void save(ServerConfig config) {
        try {
            Files.createDirectories(SERVER_FILE.getParent());
            JsonElement json = ServerConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            Files.writeString(SERVER_FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (IOException | RuntimeException e) {
            TalkingVillagers.LOG.error("Cannot write {}", SERVER_FILE, e);
        }
    }
}

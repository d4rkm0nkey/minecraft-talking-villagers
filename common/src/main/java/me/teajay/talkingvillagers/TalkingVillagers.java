package me.teajay.talkingvillagers;

import me.teajay.talkingvillagers.client.ClientVoices;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import me.teajay.talkingvillagers.server.ServerVoices;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader independent entry point. The fabric and neoforge entry points delegate here.
 */
public final class TalkingVillagers {
    public static final String MOD_ID = "talkingvillagers";
    public static final String MOD_NAME = "Talking Villagers";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private TalkingVillagers() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        LOG.info("{} loaded on {} ({})", MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        Configs.load();
        Voicepacks.onReload(ServerVoices::onPacksReloaded);
        Voicepacks.reload();
    }

    /**
     * Client side setup, called after {@link #init()}.
     */
    public static void initClient() {
        Configs.loadClient();
        Voicepacks.onReload(ClientVoices::onLocalPacksReloaded);
    }
}

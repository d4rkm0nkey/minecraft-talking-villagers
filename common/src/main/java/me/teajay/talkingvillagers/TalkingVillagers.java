package me.teajay.talkingvillagers;

import me.teajay.talkingvillagers.platform.Services;
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

    public static void init() {
        LOG.info("{} loaded on {} ({})", MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
    }
}

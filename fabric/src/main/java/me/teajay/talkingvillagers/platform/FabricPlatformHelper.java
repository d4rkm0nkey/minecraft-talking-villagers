package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.platform.services.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public String getModVersion() {
        return FabricLoader.getInstance().getModContainer(TalkingVillagers.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
}

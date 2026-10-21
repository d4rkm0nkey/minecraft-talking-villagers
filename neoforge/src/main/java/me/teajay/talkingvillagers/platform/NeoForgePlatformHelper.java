package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.platform.services.PlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById(TalkingVillagers.MOD_ID)
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("");
    }
}

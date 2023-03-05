package me.teajay.talkingvillagers;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.teajay.talkingvillagers.common.sound.VoiceManager;
import me.teajay.talkingvillagers.config.ModConfig;
import me.teajay.talkingvillagers.gui.MenuSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

// ToDo Reputation based voice lines
public class TalkingVillagers implements ModInitializer {
	public static final String MODID = "talkingvillagers";
	public static final Logger LOGGER = LogManager.getLogger("Mod Menu");

	public static final Path CONFIG_DIRECTORY = Path.of(FabricLoader.getInstance().getConfigDir().toString(), MODID);
	public static final Path VOICEPACK_DIRECTORY = Path.of(FabricLoader.getInstance().getGameDir().toString(), "resourcepacks");

	@Override
	public void onInitialize() {
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		VoiceManager.initialiseVoices();
		MenuSounds.initializeSounds();
	}
}
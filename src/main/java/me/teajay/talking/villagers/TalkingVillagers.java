package me.teajay.talking.villagers;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;
import me.teajay.talking.villagers.common.data.ResourceLoader;
import net.fabricmc.api.ModInitializer;

public class TalkingVillagers implements ModInitializer {
	public static final String MODID = "talkingvillagers";
	@Override
	public void onInitialize() {
		ResourceLoader.initLoader();
		VillagerVoiceManager.initialiseVoices();
	}
}
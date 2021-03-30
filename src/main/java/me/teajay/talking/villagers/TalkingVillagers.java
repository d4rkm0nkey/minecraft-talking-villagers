package me.teajay.talking.villagers;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;
import net.fabricmc.api.ModInitializer;

public class TalkingVillagers implements ModInitializer {
	public static final String MODID = "talkingvillagers";
	@Override
	public void onInitialize() {
		VillagerVoiceManager.initialiseVoices();
	}
}
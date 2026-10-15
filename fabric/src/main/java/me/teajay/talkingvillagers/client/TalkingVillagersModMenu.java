package me.teajay.talkingvillagers.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.teajay.talkingvillagers.client.gui.VoicepacksScreen;

/**
 * Opens the voicepack manager from Mod Menu's config button.
 */
public class TalkingVillagersModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VoicepacksScreen::new;
    }
}

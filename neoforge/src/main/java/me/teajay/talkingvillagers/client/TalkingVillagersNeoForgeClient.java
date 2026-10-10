package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.client.gui.VoicepacksScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TalkingVillagers.MOD_ID, dist = Dist.CLIENT)
public class TalkingVillagersNeoForgeClient {
    public TalkingVillagersNeoForgeClient(ModContainer container) {
        TalkingVillagers.initClient();
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new VoicepacksScreen(parent));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientVoices.onDisconnect());
    }
}

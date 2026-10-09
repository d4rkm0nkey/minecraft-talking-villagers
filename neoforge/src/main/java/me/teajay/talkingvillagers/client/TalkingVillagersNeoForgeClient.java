package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TalkingVillagers.MOD_ID, dist = Dist.CLIENT)
public class TalkingVillagersNeoForgeClient {
    public TalkingVillagersNeoForgeClient() {
        TalkingVillagers.initClient();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientVoices.onDisconnect());
    }
}

package me.teajay.talkingvillagers;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TalkingVillagers.MOD_ID)
public class TalkingVillagersNeoForge {
    public TalkingVillagersNeoForge(IEventBus eventBus) {
        TalkingVillagers.init();
    }
}

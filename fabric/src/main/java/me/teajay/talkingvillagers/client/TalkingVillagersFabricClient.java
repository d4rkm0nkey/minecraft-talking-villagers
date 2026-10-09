package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.network.SpeakPayload;
import me.teajay.talkingvillagers.platform.FabricPlatformNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TalkingVillagersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TalkingVillagers.initClient();
        FabricPlatformNetworking.setClientSender(ClientPlayNetworking::send);

        ClientPlayNetworking.registerGlobalReceiver(SpeakPayload.TYPE, (payload, context) -> ClientVoices.onSpeak(payload));
        ClientPlayNetworking.registerGlobalReceiver(ServerPacksPayload.TYPE, (payload, context) -> ClientVoices.onServerPacks(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientVoices.onDisconnect());
    }
}

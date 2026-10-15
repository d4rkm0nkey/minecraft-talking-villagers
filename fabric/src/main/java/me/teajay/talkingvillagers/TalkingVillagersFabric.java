package me.teajay.talkingvillagers;

import me.teajay.talkingvillagers.command.TalkingVillagersCommand;
import me.teajay.talkingvillagers.network.ClientPacksPayload;
import me.teajay.talkingvillagers.network.PackChunkPayload;
import me.teajay.talkingvillagers.network.RequestPackPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.network.SpeakPayload;
import me.teajay.talkingvillagers.server.ServerVoices;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class TalkingVillagersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TalkingVillagers.init();

        PayloadTypeRegistry.clientboundPlay().register(SpeakPayload.TYPE, SpeakPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerPacksPayload.TYPE, ServerPacksPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PackChunkPayload.TYPE, PackChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientPacksPayload.TYPE, ClientPacksPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestPackPayload.TYPE, RequestPackPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ClientPacksPayload.TYPE, (payload, context) -> ServerVoices.onClientPacks(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(RequestPackPayload.TYPE, (payload, context) -> ServerVoices.onPackRequest(context.player(), payload));

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> TalkingVillagersCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(ServerVoices::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerVoices.onServerStopped());
        ServerTickEvents.END_SERVER_TICK.register(ServerVoices::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerVoices.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerVoices.onPlayerLeave(handler.player));
    }
}

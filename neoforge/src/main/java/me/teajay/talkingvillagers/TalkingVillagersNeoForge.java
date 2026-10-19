package me.teajay.talkingvillagers;

import me.teajay.talkingvillagers.client.ClientVoices;
import me.teajay.talkingvillagers.client.PackDownloads;
import me.teajay.talkingvillagers.command.TalkingVillagersCommand;
import me.teajay.talkingvillagers.network.ClientPacksPayload;
import me.teajay.talkingvillagers.network.PackChunkPayload;
import me.teajay.talkingvillagers.network.RequestPackPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.network.SpeakPayload;
import me.teajay.talkingvillagers.network.VoiceIdentityPayload;
import me.teajay.talkingvillagers.platform.NeoForgePlatformAttachments;
import me.teajay.talkingvillagers.server.ServerVoices;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(TalkingVillagers.MOD_ID)
public class TalkingVillagersNeoForge {
    private static final String PROTOCOL_VERSION = "1";

    public TalkingVillagersNeoForge(IEventBus modBus) {
        TalkingVillagers.init();
        NeoForgePlatformAttachments.TYPES.register(modBus);
        modBus.addListener(TalkingVillagersNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> TalkingVillagersCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> ServerVoices.onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ServerVoices.onServerStopped());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ServerVoices.onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerVoices.onPlayerJoin(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                NeoForgePlatformAttachments.onStartTracking(event.getTarget(), player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerVoices.onPlayerLeave(player);
            }
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional: players without the mod can still join and hear vanilla sounds
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        // the client handlers only run on the client, so the client classes are never loaded on a dedicated server
        registrar.playToClient(SpeakPayload.TYPE, SpeakPayload.STREAM_CODEC, (payload, context) -> ClientVoices.onSpeak(payload));
        registrar.playToClient(ServerPacksPayload.TYPE, ServerPacksPayload.STREAM_CODEC, (payload, context) -> ClientVoices.onServerPacks(payload));
        registrar.playToClient(VoiceIdentityPayload.TYPE, VoiceIdentityPayload.STREAM_CODEC, (payload, context) -> ClientVoices.onIdentity(payload));
        registrar.playToClient(PackChunkPayload.TYPE, PackChunkPayload.STREAM_CODEC, (payload, context) -> PackDownloads.onChunk(payload));
        registrar.playToServer(RequestPackPayload.TYPE, RequestPackPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerVoices.onPackRequest(player, payload);
            }
        });
        registrar.playToServer(ClientPacksPayload.TYPE, ClientPacksPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerVoices.onClientPacks(player, payload);
            }
        });
    }
}

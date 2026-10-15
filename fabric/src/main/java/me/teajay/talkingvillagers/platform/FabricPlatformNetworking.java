package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.platform.services.PlatformNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class FabricPlatformNetworking implements PlatformNetworking {
    /** Set by the client entry point, so this class never references client classes itself. */
    private static Consumer<CustomPacketPayload> clientSender = payload -> {
        throw new IllegalStateException("Cannot send to the server from a dedicated server");
    };

    public static void setClientSender(Consumer<CustomPacketPayload> sender) {
        clientSender = sender;
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }
}

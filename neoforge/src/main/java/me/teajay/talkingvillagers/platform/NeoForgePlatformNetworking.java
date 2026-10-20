package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.platform.services.PlatformNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class NeoForgePlatformNetworking implements PlatformNetworking {
    /** Set by the client entry point, so this class never references client classes itself. */
    private static Consumer<CustomPacketPayload> clientSender = payload -> {
        throw new IllegalStateException("Cannot send to the server from a dedicated server");
    };

    public static void setClientSender(Consumer<CustomPacketPayload> sender) {
        clientSender = sender;
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }
}

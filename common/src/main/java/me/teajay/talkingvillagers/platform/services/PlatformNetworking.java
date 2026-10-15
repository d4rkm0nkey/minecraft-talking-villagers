package me.teajay.talkingvillagers.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends the mod's payloads. Registration of payload types and handlers happens in the loader entry points.
 */
public interface PlatformNetworking {
    /**
     * Whether the player's client has the mod and accepts this payload.
     */
    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * Client side only.
     */
    void sendToServer(CustomPacketPayload payload);
}

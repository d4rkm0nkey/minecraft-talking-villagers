package me.teajay.talkingvillagers.network;

import me.teajay.talkingvillagers.TalkingVillagers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Set;

/**
 * Client to server: which of the server's packs the client has, identified by SHA-256.
 * The server only sends lines from these packs and vanilla sounds otherwise.
 */
public record ClientPacksPayload(Set<String> packHashes) implements CustomPacketPayload {
    public static final Type<ClientPacksPayload> TYPE = new Type<>(TalkingVillagers.id("client_packs"));
    public static final StreamCodec<ByteBuf, ClientPacksPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(64)
            .apply(ByteBufCodecs.list(ServerPacksPayload.MAX_PACKS))
            .map(hashes -> new ClientPacksPayload(Set.copyOf(hashes)), payload -> List.copyOf(payload.packHashes()));

    public ClientPacksPayload {
        packHashes = Set.copyOf(packHashes);
    }

    @Override
    public Type<ClientPacksPayload> type() {
        return TYPE;
    }
}

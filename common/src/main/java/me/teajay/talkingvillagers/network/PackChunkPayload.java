package me.teajay.talkingvillagers.network;

import io.netty.buffer.ByteBuf;
import me.teajay.talkingvillagers.TalkingVillagers;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a piece of a pack file. Pieces are small enough for the custom payload size limit.
 */
public record PackChunkPayload(String sha256, long offset, long total, byte[] data) implements CustomPacketPayload {
    public static final int MAX_CHUNK = 64 * 1024;
    public static final Type<PackChunkPayload> TYPE = new Type<>(TalkingVillagers.id("pack_chunk"));
    public static final StreamCodec<ByteBuf, PackChunkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PackChunkPayload::sha256,
            ByteBufCodecs.VAR_LONG, PackChunkPayload::offset,
            ByteBufCodecs.VAR_LONG, PackChunkPayload::total,
            ByteBufCodecs.byteArray(MAX_CHUNK), PackChunkPayload::data,
            PackChunkPayload::new);

    @Override
    public Type<PackChunkPayload> type() {
        return TYPE;
    }
}

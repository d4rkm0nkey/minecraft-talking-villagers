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
    /**
     * {@link #total} of an empty chunk that tells the client the server will not send the pack. Older clients see a
     * size that does not match and give up as well.
     */
    public static final long REFUSED = -1;
    public static final Type<PackChunkPayload> TYPE = new Type<>(TalkingVillagers.id("pack_chunk"));
    public static final StreamCodec<ByteBuf, PackChunkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PackChunkPayload::sha256,
            ByteBufCodecs.VAR_LONG, PackChunkPayload::offset,
            ByteBufCodecs.VAR_LONG, PackChunkPayload::total,
            ByteBufCodecs.byteArray(MAX_CHUNK), PackChunkPayload::data,
            PackChunkPayload::new);

    public static PackChunkPayload refused(String sha256) {
        return new PackChunkPayload(sha256, 0, REFUSED, new byte[0]);
    }

    @Override
    public Type<PackChunkPayload> type() {
        return TYPE;
    }
}

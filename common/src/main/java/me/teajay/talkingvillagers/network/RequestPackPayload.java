package me.teajay.talkingvillagers.network;

import io.netty.buffer.ByteBuf;
import me.teajay.talkingvillagers.TalkingVillagers;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: please send the pack with this SHA-256.
 */
public record RequestPackPayload(String sha256) implements CustomPacketPayload {
    public static final Type<RequestPackPayload> TYPE = new Type<>(TalkingVillagers.id("request_pack"));
    public static final StreamCodec<ByteBuf, RequestPackPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(64)
            .map(RequestPackPayload::new, RequestPackPayload::sha256);

    @Override
    public Type<RequestPackPayload> type() {
        return TYPE;
    }
}

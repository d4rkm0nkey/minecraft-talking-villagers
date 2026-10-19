package me.teajay.talkingvillagers.network;

import me.teajay.talkingvillagers.TalkingVillagers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: the voicepacks the server uses. The client answers with {@link ClientPacksPayload}.
 */
public record ServerPacksPayload(List<PackInfo> packs) implements CustomPacketPayload {
    public static final int MAX_PACKS = 1024;
    public static final int MAX_NAME = 256;
    public static final int MAX_VERSION = 64;
    public static final int MAX_URL = 2048;
    public static final Type<ServerPacksPayload> TYPE = new Type<>(TalkingVillagers.id("server_packs"));
    public static final StreamCodec<ByteBuf, ServerPacksPayload> STREAM_CODEC = PackInfo.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_PACKS))
            .map(ServerPacksPayload::new, ServerPacksPayload::packs);

    public ServerPacksPayload {
        packs = List.copyOf(packs);
    }

    /**
     * @param size         file size in bytes, for the download dialog
     * @param downloadable the server sends this pack on request
     * @param url          HTTP(S) address to download the pack from instead, or empty
     */
    public record PackInfo(String id, String name, String version, String sha256, long size, boolean downloadable, String url) {
        public static final StreamCodec<ByteBuf, PackInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), PackInfo::id,
                ByteBufCodecs.stringUtf8(MAX_NAME), PackInfo::name,
                ByteBufCodecs.stringUtf8(MAX_VERSION), PackInfo::version,
                ByteBufCodecs.stringUtf8(64), PackInfo::sha256,
                ByteBufCodecs.VAR_LONG, PackInfo::size,
                ByteBufCodecs.BOOL, PackInfo::downloadable,
                ByteBufCodecs.stringUtf8(MAX_URL), PackInfo::url,
                PackInfo::new);
    }

    @Override
    public Type<ServerPacksPayload> type() {
        return TYPE;
    }
}

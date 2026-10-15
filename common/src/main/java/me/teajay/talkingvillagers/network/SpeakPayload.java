package me.teajay.talkingvillagers.network;

import me.teajay.talkingvillagers.TalkingVillagers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: an entity says a line.
 *
 * @param packHash  SHA-256 of the voicepack file, so the client plays the line from exactly the same file
 * @param lineIndex index into the voice's lines of {@code eventId}
 */
public record SpeakPayload(int entityId, String packHash, String voiceId, String eventId, int lineIndex, float pitch, float volume)
        implements CustomPacketPayload {
    public static final Type<SpeakPayload> TYPE = new Type<>(TalkingVillagers.id("speak"));
    public static final StreamCodec<ByteBuf, SpeakPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpeakPayload::entityId,
            ByteBufCodecs.stringUtf8(64), SpeakPayload::packHash,
            ByteBufCodecs.stringUtf8(64), SpeakPayload::voiceId,
            ByteBufCodecs.stringUtf8(64), SpeakPayload::eventId,
            ByteBufCodecs.VAR_INT, SpeakPayload::lineIndex,
            ByteBufCodecs.FLOAT, SpeakPayload::pitch,
            ByteBufCodecs.FLOAT, SpeakPayload::volume,
            SpeakPayload::new);

    @Override
    public Type<SpeakPayload> type() {
        return TYPE;
    }
}

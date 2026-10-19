package me.teajay.talkingvillagers.network;

import io.netty.buffer.ByteBuf;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.voice.VoiceIdentityCodecs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the voice of an entity. Only used on NeoForge, where a synced attachment would make the mod
 * required on clients; Fabric syncs the attachment itself.
 */
public record VoiceIdentityPayload(int entityId, VoiceIdentity identity) implements CustomPacketPayload {
    public static final Type<VoiceIdentityPayload> TYPE = new Type<>(TalkingVillagers.id("voice_identity"));
    public static final StreamCodec<ByteBuf, VoiceIdentityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoiceIdentityPayload::entityId,
            VoiceIdentityCodecs.STREAM_CODEC, VoiceIdentityPayload::identity,
            VoiceIdentityPayload::new);

    @Override
    public Type<VoiceIdentityPayload> type() {
        return TYPE;
    }
}

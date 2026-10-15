package me.teajay.talkingvillagers.voice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Save format and network format of {@link VoiceIdentity}.
 */
public final class VoiceIdentityCodecs {
    public static final Codec<Gender> GENDER = Codec.STRING.comapFlatMap(
            id -> Gender.byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown gender " + id)),
            Gender::id);
    public static final Codec<VoiceRef> VOICE_REF = Codec.STRING.comapFlatMap(
            value -> VoiceRef.parse(value).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Invalid voice " + value)),
            VoiceRef::toString);

    public static final MapCodec<VoiceIdentity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GENDER.fieldOf("gender").forGetter(VoiceIdentity::gender),
            VOICE_REF.optionalFieldOf("voice").forGetter(VoiceIdentity::voice),
            Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(VoiceIdentity::pitch),
            Codec.BOOL.optionalFieldOf("child", false).forGetter(VoiceIdentity::child),
            Codec.STRING.optionalFieldOf("language").forGetter(VoiceIdentity::language),
            Codec.BOOL.optionalFieldOf("manual", false).forGetter(VoiceIdentity::manual),
            Codec.BOOL.optionalFieldOf("muted", false).forGetter(VoiceIdentity::muted)
    ).apply(instance, VoiceIdentity::new));
    public static final Codec<VoiceIdentity> CODEC = MAP_CODEC.codec();

    private static final StreamCodec<ByteBuf, Gender> GENDER_STREAM = ByteBufCodecs.VAR_INT.map(id -> Gender.values()[id], Gender::ordinal);
    private static final StreamCodec<ByteBuf, VoiceRef> VOICE_REF_STREAM = ByteBufCodecs.stringUtf8(129)
            .map(value -> VoiceRef.parse(value).orElseThrow(), VoiceRef::toString);

    /** Clients only need what decides whether vanilla sounds are replaced. */
    public static final StreamCodec<ByteBuf, VoiceIdentity> STREAM_CODEC = StreamCodec.composite(
            GENDER_STREAM, VoiceIdentity::gender,
            ByteBufCodecs.optional(VOICE_REF_STREAM), VoiceIdentity::voice,
            ByteBufCodecs.FLOAT, VoiceIdentity::pitch,
            ByteBufCodecs.BOOL, VoiceIdentity::child,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(64)), VoiceIdentity::language,
            ByteBufCodecs.BOOL, VoiceIdentity::manual,
            ByteBufCodecs.BOOL, VoiceIdentity::muted,
            VoiceIdentity::new);

    private VoiceIdentityCodecs() {
    }

    /** Placeholder for loaders that need a default value; never stored on an entity. */
    public static VoiceIdentity placeholder() {
        return VoiceIdentity.withoutVoice(Gender.NEUTRAL, false, Optional.empty());
    }
}

package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.platform.services.PlatformAttachments;
import me.teajay.talkingvillagers.voice.VoiceIdentityCodecs;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public class NeoForgePlatformAttachments implements PlatformAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TalkingVillagers.MOD_ID);
    public static final Supplier<AttachmentType<VoiceIdentity>> VOICE = TYPES.register("voice", () -> AttachmentType
            .builder(VoiceIdentityCodecs::placeholder)
            .serialize(VoiceIdentityCodecs.MAP_CODEC)
            .sync(VoiceIdentityCodecs.STREAM_CODEC)
            .build());

    @Override
    public Optional<VoiceIdentity> getIdentity(Entity entity) {
        return entity.getExistingData(VOICE);
    }

    @Override
    public void setIdentity(Entity entity, VoiceIdentity identity) {
        entity.setData(VOICE, identity);
    }
}

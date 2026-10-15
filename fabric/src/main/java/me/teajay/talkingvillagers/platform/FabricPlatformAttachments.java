package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.platform.services.PlatformAttachments;
import me.teajay.talkingvillagers.voice.VoiceIdentityCodecs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class FabricPlatformAttachments implements PlatformAttachments {
    public static final AttachmentType<VoiceIdentity> VOICE = AttachmentRegistry.create(TalkingVillagers.id("voice"), builder -> builder
            .persistent(VoiceIdentityCodecs.CODEC)
            .syncWith(VoiceIdentityCodecs.STREAM_CODEC, AttachmentSyncPredicate.all()));

    @Override
    public Optional<VoiceIdentity> getIdentity(Entity entity) {
        return Optional.ofNullable(entity.getAttached(VOICE));
    }

    @Override
    public void setIdentity(Entity entity, VoiceIdentity identity) {
        entity.setAttached(VOICE, identity);
    }
}

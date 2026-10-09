package me.teajay.talkingvillagers.platform.services;

import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * Stores the {@link VoiceIdentity} on entities. Saved with the entity and synced to clients tracking it.
 */
public interface PlatformAttachments {
    Optional<VoiceIdentity> getIdentity(Entity entity);

    void setIdentity(Entity entity, VoiceIdentity identity);
}

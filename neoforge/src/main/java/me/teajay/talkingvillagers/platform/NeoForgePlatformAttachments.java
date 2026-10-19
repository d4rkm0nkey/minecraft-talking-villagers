package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.network.VoiceIdentityPayload;
import me.teajay.talkingvillagers.platform.services.PlatformAttachments;
import me.teajay.talkingvillagers.voice.VoiceIdentityCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public class NeoForgePlatformAttachments implements PlatformAttachments {
    /** Players farther away than this don't track the entity, they get its voice when they start tracking it. */
    private static final double SYNC_RANGE = 160;
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TalkingVillagers.MOD_ID);
    // not synced by NeoForge: synced attachment types are a synced registry, so clients without the mod could not join
    public static final Supplier<AttachmentType<VoiceIdentity>> VOICE = TYPES.register("voice", () -> AttachmentType
            .builder(VoiceIdentityCodecs::placeholder)
            .serialize(VoiceIdentityCodecs.MAP_CODEC)
            // also copied when a villager turns into a zombie villager and back, or into a witch
            .copyOnDeath()
            .build());

    @Override
    public Optional<VoiceIdentity> getIdentity(Entity entity) {
        return entity.getExistingData(VOICE);
    }

    @Override
    public void setIdentity(Entity entity, VoiceIdentity identity) {
        entity.setData(VOICE, identity);
        if (entity.level() instanceof ServerLevel level) {
            VoiceIdentityPayload payload = new VoiceIdentityPayload(entity.getId(), identity);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(entity) < SYNC_RANGE * SYNC_RANGE && player.connection.hasChannel(VoiceIdentityPayload.TYPE)) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }

    /** A player starts tracking the entity, e.g. it came into view distance. */
    public static void onStartTracking(Entity entity, ServerPlayer player) {
        entity.getExistingData(VOICE).ifPresent(identity -> {
            if (player.connection.hasChannel(VoiceIdentityPayload.TYPE)) {
                PacketDistributor.sendToPlayer(player, new VoiceIdentityPayload(entity.getId(), identity));
            }
        });
    }
}

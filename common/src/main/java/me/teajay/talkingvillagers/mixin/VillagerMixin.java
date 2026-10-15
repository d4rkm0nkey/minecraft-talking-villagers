package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import me.teajay.talkingvillagers.voice.LegacyVoices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Shadow
    private long lastGossipTime;

    private Villager talkingvillagers$self() {
        return (Villager) (Object) this;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void talkingvillagers$tick(ServerLevel level, CallbackInfo ci) {
        EntityVoices.tickChatter(talkingvillagers$self(), level);
    }

    @Inject(method = "increaseMerchantCareer", at = @At("TAIL"))
    private void talkingvillagers$levelUp(ServerLevel level, CallbackInfo ci) {
        EntityVoices.onEvent(talkingvillagers$self(), EventCatalog.LEVEL_UP);
    }

    @Inject(method = "gossip", at = @At("TAIL"))
    private void talkingvillagers$gossip(ServerLevel level, Villager target, long timestamp, CallbackInfo ci) {
        // vanilla only gossips if neither villager gossiped in the last minute
        if (lastGossipTime == timestamp) {
            EntityVoices.onEvent(talkingvillagers$self(), EventCatalog.GOSSIP);
        }
    }

    @Inject(method = "getBreedOffspring", at = @At("RETURN"))
    private void talkingvillagers$birth(ServerLevel level, AgeableMob partner, CallbackInfoReturnable<Villager> cir) {
        Villager child = cir.getReturnValue();
        if (child != null) {
            EntityVoices.onBirth(child, talkingvillagers$self(), partner);
        }
    }

    @Inject(method = "ageBoundaryReached", at = @At("HEAD"))
    private void talkingvillagers$growUp(CallbackInfo ci) {
        Villager self = talkingvillagers$self();
        if (!self.level().isClientSide() && !self.isBaby()) {
            EntityVoices.onGrowUp(self);
        }
    }

    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void talkingvillagers$wakeUp(CallbackInfo ci) {
        Villager self = talkingvillagers$self();
        if (!self.level().isClientSide() && self.isAlive() && self.level().isBrightOutside()) {
            EntityVoices.onEvent(self, EventCatalog.WAKE_UP);
        }
    }

    @Inject(method = "onReputationEventFrom", at = @At("TAIL"))
    private void talkingvillagers$reputationEvent(ReputationEventType type, Entity source, CallbackInfo ci) {
        if (type == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
            EntityVoices.onEvent(talkingvillagers$self(), EventCatalog.CURED);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void talkingvillagers$migrateV1(ValueInput input, CallbackInfo ci) {
        LegacyVoices.migrate(talkingvillagers$self(), input);
    }
}

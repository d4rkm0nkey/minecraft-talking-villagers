package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import me.teajay.talkingvillagers.voice.LegacyVoices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Shadow
    private long lastGossipTime;

    private Villager talkingvillagers$self() {
        return (Villager) (Object) this;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void talkingvillagers$tick(ServerLevel level, CallbackInfo ci) {
        EntityVoices.tickVillager(talkingvillagers$self(), level);
    }

    @Inject(method = "startTrading", at = @At("TAIL"))
    private void talkingvillagers$trade(Player player, CallbackInfo ci) {
        EntityVoices.onEvent(talkingvillagers$self(), EventCatalog.TRADE);
    }

    @Inject(method = "rewardTradeXp", at = @At("HEAD"))
    private void talkingvillagers$tradeSuccess(MerchantOffer offer, CallbackInfo ci) {
        EntityVoices.onEvent(talkingvillagers$self(), EventCatalog.TRADE_SUCCESS);
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

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void talkingvillagers$migrateV1(ValueInput input, CallbackInfo ci) {
        LegacyVoices.migrate(talkingvillagers$self(), input);
    }
}

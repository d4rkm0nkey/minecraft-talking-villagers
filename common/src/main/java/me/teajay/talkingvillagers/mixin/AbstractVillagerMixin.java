package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trading events shared by villagers and wandering traders.
 */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Inject(method = "setTradingPlayer", at = @At("HEAD"))
    private void talkingvillagers$trade(@Nullable Player player, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (player != null && self.getTradingPlayer() == null && !self.level().isClientSide()) {
            EntityVoices.onEvent(self, EventCatalog.TRADE);
        }
    }

    @Inject(method = "notifyTrade", at = @At("HEAD"))
    private void talkingvillagers$tradeSuccess(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!self.level().isClientSide()) {
            EntityVoices.onEvent(self, EventCatalog.TRADE_SUCCESS);
        }
    }
}

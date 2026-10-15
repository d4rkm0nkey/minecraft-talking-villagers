package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.world.entity.monster.illager.Pillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pillager.class)
public abstract class PillagerMixin {
    @Inject(method = "setChargingCrossbow", at = @At("HEAD"))
    private void talkingvillagers$charge(boolean charging, CallbackInfo ci) {
        Pillager self = (Pillager) (Object) this;
        if (charging && !self.isChargingCrossbow() && !self.level().isClientSide()) {
            EntityVoices.onEvent(self, EventCatalog.CHARGE);
        }
    }
}

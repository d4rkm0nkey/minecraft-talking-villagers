package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void talkingvillagers$tick(CallbackInfo ci) {
        WanderingTrader self = (WanderingTrader) (Object) this;
        if (self.level() instanceof ServerLevel level) {
            EntityVoices.tickChatter(self, level);
        }
    }
}

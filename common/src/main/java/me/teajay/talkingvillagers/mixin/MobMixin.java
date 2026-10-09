package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "getAmbientSoundInterval", at = @At("RETURN"), cancellable = true)
    private void talkingvillagers$rarerAmbientLines(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(EntityVoices.ambientInterval((Mob) (Object) this, cir.getReturnValueI()));
    }
}

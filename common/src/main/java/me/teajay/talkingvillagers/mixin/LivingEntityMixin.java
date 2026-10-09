package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Ambient, hurt, death and most villager sounds go through {@code makeSound}; a voice line replaces them.
     */
    @Inject(method = "makeSound", at = @At("HEAD"), cancellable = true)
    private void talkingvillagers$replaceSound(@Nullable SoundEvent sound, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && EntityVoices.onVanillaSound(self, sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "startSleeping", at = @At("TAIL"))
    private void talkingvillagers$sleep(BlockPos bedPosition, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cir.getReturnValueZ() && !self.level().isClientSide() && EntityVoices.kindOf(self).isPresent()) {
            EntityVoices.onEvent(self, EventCatalog.SLEEP);
        }
    }
}

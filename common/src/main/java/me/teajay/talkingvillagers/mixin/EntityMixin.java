package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Ambient, hurt, death, trade, celebrate and spell sounds all end up here; a voice line replaces them.
     */
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void talkingvillagers$replaceSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity self && !self.level().isClientSide() && EntityVoices.onVanillaSound(self, sound)) {
            ci.cancel();
        }
    }
}

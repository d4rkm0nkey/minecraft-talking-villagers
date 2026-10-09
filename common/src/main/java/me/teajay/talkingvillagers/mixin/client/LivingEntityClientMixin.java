package me.teajay.talkingvillagers.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.teajay.talkingvillagers.client.ClientVoices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client plays hurt and death sounds itself when the server reports damage. For entities whose voice this client
 * can play, the server sends the voice line instead, so the local vanilla sound is skipped.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {
    @WrapOperation(method = "handleDamageEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent talkingvillagers$skipHurtSound(LivingEntity entity, DamageSource source, Operation<SoundEvent> original) {
        return ClientVoices.isVoiced(entity) ? null : original.call(entity, source);
    }

    @WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getDeathSound()Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent talkingvillagers$skipDeathSound(LivingEntity entity, Operation<SoundEvent> original) {
        return ClientVoices.isVoiced(entity) ? null : original.call(entity);
    }
}

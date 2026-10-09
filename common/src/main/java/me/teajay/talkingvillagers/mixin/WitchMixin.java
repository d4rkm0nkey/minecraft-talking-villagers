package me.teajay.talkingvillagers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Witches play their drink and throw sounds directly on the level instead of through the entity.
 */
@Mixin(Witch.class)
public abstract class WitchMixin {
    @WrapOperation(method = {"aiStep", "performRangedAttack"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void talkingvillagers$replacePotionSound(Level level, Entity except, double x, double y, double z, SoundEvent sound,
                                                     SoundSource source, float volume, float pitch, Operation<Void> original) {
        if (level.isClientSide() || !EntityVoices.onVanillaSound((Witch) (Object) this, sound)) {
            original.call(level, except, x, y, z, sound, source, volume, pitch);
        }
    }
}

package me.teajay.talkingvillagers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Witches play their drink and throw sounds directly on the level instead of through the entity. The line depends on
 * the potion: healing herself, another potion, or healing an allied raider.
 */
@Mixin(Witch.class)
public abstract class WitchMixin {
    private static final String LEVEL_PLAY_SOUND =
            "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V";

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = LEVEL_PLAY_SOUND))
    private void talkingvillagers$drink(Level level, Entity except, double x, double y, double z, SoundEvent sound,
                                        SoundSource source, float volume, float pitch, Operation<Void> original) {
        Witch self = (Witch) (Object) this;
        // the potion is already in her hand when the sound plays
        PotionContents potion = self.getMainHandItem().get(DataComponents.POTION_CONTENTS);
        boolean healing = potion != null && potion.is(Potions.HEALING);
        if (level.isClientSide() || !EntityVoices.onEvent(self, healing ? EventCatalog.HEAL_SELF : EventCatalog.DRINK_OTHER, sound)) {
            original.call(level, except, x, y, z, sound, source, volume, pitch);
        }
    }

    @WrapOperation(method = "performRangedAttack", at = @At(value = "INVOKE", target = LEVEL_PLAY_SOUND))
    private void talkingvillagers$throw(Level level, Entity except, double x, double y, double z, SoundEvent sound,
                                        SoundSource source, float volume, float pitch, Operation<Void> original,
                                        @Local(argsOnly = true) LivingEntity target) {
        Witch self = (Witch) (Object) this;
        boolean healsAlly = target instanceof Raider;
        if (level.isClientSide() || !EntityVoices.onEvent(self, healsAlly ? EventCatalog.HEAL_ALLY : EventCatalog.THROW_POTION, sound)) {
            original.call(level, except, x, y, z, sound, source, volume, pitch);
        }
    }
}

package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.common.util.IVillagerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.GiveGiftsToHeroTask;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GiveGiftsToHeroTask.class)
public class GiveGiftsToHeroTaskMixin {
    @Inject(at = @At("HEAD"), method = "giveGifts(Lnet/minecraft/entity/passive/VillagerEntity;Lnet/minecraft/entity/LivingEntity;)V")
    private void giveGifts(VillagerEntity villager, LivingEntity recipient, CallbackInfo ci) {
        ((IVillagerEntity)villager).sayHeroDrop();
    }
}

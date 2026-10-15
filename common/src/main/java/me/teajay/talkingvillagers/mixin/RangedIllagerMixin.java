package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Pillager.class, Illusioner.class})
public abstract class RangedIllagerMixin {
    @Inject(method = "performRangedAttack", at = @At("HEAD"))
    private void talkingvillagers$attack(LivingEntity target, float power, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            EntityVoices.onEvent(self, EventCatalog.ATTACK);
        }
    }
}

package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected boolean dead;

    @Inject(method = "startSleeping", at = @At("RETURN"))
    private void talkingvillagers$sleep(BlockPos bedPosition, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cir.getReturnValueZ() && !self.level().isClientSide()) {
            EntityVoices.onEvent(self, EventCatalog.SLEEP);
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void talkingvillagers$hurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            EntityVoices.onHurt((LivingEntity) (Object) this, source);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void talkingvillagers$killed(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // same check as vanilla, which ignores die() on entities that are already dead, e.g. when another mod calls it again
        if (!self.isRemoved() && !dead) {
            EntityVoices.onDeath(self, source);
        }
    }
}

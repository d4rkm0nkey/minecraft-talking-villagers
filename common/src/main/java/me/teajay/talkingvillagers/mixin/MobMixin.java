package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.voice.EntityVoices;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    /**
     * Illagers react when they spot a player.
     */
    @Inject(method = "setTarget", at = @At("HEAD"))
    private void talkingvillagers$spotPlayer(@Nullable LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target instanceof Player && !(self.getTarget() instanceof Player) && !self.level().isClientSide()) {
            // vindicators storm towards their target
            EntityVoices.onEvent(self, self instanceof Vindicator ? EventCatalog.CHARGE_AT : EventCatalog.SPOT_PLAYER);
        }
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void talkingvillagers$tick(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level() instanceof ServerLevel level) {
            EntityVoices.tickHostile(self, level);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void talkingvillagers$attack(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            EntityVoices.onEvent((Mob) (Object) this, EventCatalog.ATTACK);
        }
    }

    @Inject(method = "getAmbientSoundInterval", at = @At("RETURN"), cancellable = true)
    private void talkingvillagers$rarerAmbientLines(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(EntityVoices.ambientInterval((Mob) (Object) this, cir.getReturnValueI()));
    }
}

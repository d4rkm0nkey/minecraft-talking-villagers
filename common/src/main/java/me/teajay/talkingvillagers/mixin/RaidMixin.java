package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
public abstract class RaidMixin {
    @Inject(method = "playSound", at = @At("TAIL"))
    private void talkingvillagers$raidHorn(ServerLevel level, BlockPos soundOrigin, CallbackInfo ci) {
        EntityVoices.onRaidHorn((Raid) (Object) this);
    }
}

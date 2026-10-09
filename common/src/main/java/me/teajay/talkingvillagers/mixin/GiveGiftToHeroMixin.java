package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GiveGiftToHero.class)
public abstract class GiveGiftToHeroMixin {
    @Inject(method = "throwGift", at = @At("HEAD"))
    private void talkingvillagers$heroGift(ServerLevel level, Villager villager, LivingEntity target, CallbackInfo ci) {
        EntityVoices.onEvent(villager, EventCatalog.HERO_GIFT);
    }
}

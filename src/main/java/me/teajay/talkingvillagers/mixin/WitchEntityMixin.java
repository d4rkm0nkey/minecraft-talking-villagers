package me.teajay.talkingvillagers.mixin;

import me.teajay.talkingvillagers.common.sound.VoiceManager;
import me.teajay.talkingvillagers.common.sound.WitchVoice;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(WitchEntity.class)
public abstract class WitchEntityMixin extends RaiderEntity {
    protected Random random = new Random();
    private Float voicePitch;
    private WitchVoice voice;


    public WitchEntityMixin(EntityType<? extends WitchEntity> entityType, World world) {
        super((EntityType<? extends RaiderEntity>)entityType, world);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V")
    private void init(CallbackInfo info) {
        voicePitch = generateVoicePitch();
        voice = VoiceManager.getRandomWitchVoice();
        random = new Random();
    }
}

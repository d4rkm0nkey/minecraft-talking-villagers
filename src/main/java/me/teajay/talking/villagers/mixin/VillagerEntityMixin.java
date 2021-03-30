package me.teajay.talking.villagers.mixin;

import java.util.Random;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
	public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
	}

	private VillagerVoiceManager voiceManager;
	protected Random random;
	private static int minTimeNextRandomTalking = 500;
	private static int maxTimeNextRandomTalking = 10000;
	private int nextRandomTalking = 500 + random.nextInt(maxTimeNextRandomTalking - minTimeNextRandomTalking);

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V")
	private void init(CallbackInfo info) {
		voiceManager = new VillagerVoiceManager( ((VillagerEntity) (Object) this));
		random = new Random();
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void tick(CallbackInfo info) {
		if(nextRandomTalking <= 0) {
			voiceManager.speak(world, VillagerVoiceManager.Reason.RANDOM,this.getSoundVolume(), this.getSoundPitch());
			nextRandomTalking = 500 + random.nextInt(maxTimeNextRandomTalking - minTimeNextRandomTalking);
		} else {
			nextRandomTalking--;
		}
	}

	@Inject(at = @At("INVOKE"), method = "getDeathSound()Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getDeathSoundInject(CallbackInfoReturnable ret) {
		SoundEvent death = voiceManager.getVoice().getVoiceLine(VillagerVoiceManager.Reason.DEATH);
		if(death != null) {
			ret.setReturnValue(death);
		}
	}

	@Inject(at = @At("INVOKE"), method = "getHurtSound(Lnet/minecraft/entity/damage/DamageSource;)Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getHurtSoundInject(CallbackInfoReturnable ret) {
		SoundEvent hurt = voiceManager.getVoice().getVoiceLine(VillagerVoiceManager.Reason.HURT);
		if(hurt != null) {
			ret.setReturnValue(hurt);
		}
	}

	@Inject(at = @At("TAIL"), method = "levelUp()V")
	private void levelUp(CallbackInfo info) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.LEVEL, this.getSoundVolume(), this.getSoundPitch());
	}

	@Inject(at = @At("INVOKE"), method = "sayNo()V", cancellable = true)
	private void sayNo(CallbackInfo info) {
		SoundEvent no = voiceManager.getVoice().getVoiceLine(VillagerVoiceManager.Reason.NO);
		if(no != null && !this.world.isClient()) {
			this.setHeadRollingTimeLeft(40);
			this.playSound(no, this.getSoundVolume(), this.getSoundPitch());
			info.cancel();
		}
	}

	@Inject(at = @At("TAIL"), method = "eatForBreeding()V")
	public void eatForBreeding(CallbackInfo info) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.EAT, this.getSoundVolume(), this.getSoundPitch());
	}
}

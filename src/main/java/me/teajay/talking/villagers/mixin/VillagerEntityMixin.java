package me.teajay.talking.villagers.mixin;

import java.util.Optional;
import java.util.Random;

import me.teajay.talking.villagers.common.util.IVillagerEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
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
public abstract class VillagerEntityMixin extends MerchantEntity implements IVillagerEntity {
	private static final int MIN_RANDOM_COOLDOWN = 300;
	private static final int MAX_RANDOM_COOLDOWN = 3000;
	private static final int MIN_GREETING_COOLDOWN = 6000;
	private static final int MIN_HERO_COOLDOWN = 2000;

	private VillagerVoiceManager voiceManager;
	protected Random random = new Random();
	private Float voicePitch;
	private int nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
	private int greetingCoolDown = 0;
	private int heroCoolDown = 0;

	public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V")
	private void init(CallbackInfo info) {
		voicePitch = generateVoicePitch();
		voiceManager = new VillagerVoiceManager( ((VillagerEntity) (Object) this));
		random = new Random();
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void tick(CallbackInfo info) {
		if(greetingCoolDown > 0) {
			greetingCoolDown--;
		} else {
			Optional<PlayerEntity> close = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);

			if(close.isPresent() && isCloseEnough(((VillagerEntity) (Object)this), close.get())) {
				if(random.nextInt(20) == 0) {
					this.voiceManager.speak(world, VillagerVoiceManager.Reason.GREETING, this.getSoundVolume());
					greetingCoolDown = MIN_GREETING_COOLDOWN;
				}
			}
		}
		if(heroCoolDown < 0) {
			Optional<PlayerEntity> heroOpt = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(
					playerEntity -> playerEntity.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)
			);
			if(heroOpt.isPresent()) {
				voiceManager.speak(world, VillagerVoiceManager.Reason.HERO, this.getSoundVolume());
				heroCoolDown = MIN_HERO_COOLDOWN;
			}
		} else {
			heroCoolDown--;
		}
		if(nextRandomTalking <= 0 && !voiceManager.isTalking()) {
			if(random.nextInt(100) == 0) {
				voiceManager.speak(world, VillagerVoiceManager.Reason.RANDOM,this.getSoundVolume());
				nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
			}
		} else {
			nextRandomTalking--;
		}


	}

	@Inject(at = @At("TAIL"), method = "getAmbientSound()Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getAmbientSound(CallbackInfoReturnable<SoundEvent> ret) {
		SoundEvent ambient = voiceManager.getVoiceLine(VillagerVoiceManager.Reason.AMBIENT);
		if(ambient != null) {
			ret.setReturnValue(ambient);
		}
	}

	@Inject(at = @At("TAIL"), method = "getDeathSound()Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getDeathSoundInject(CallbackInfoReturnable<SoundEvent> ret) {
		SoundEvent death = voiceManager.getVoiceLine(VillagerVoiceManager.Reason.DEATH);
		if(death != null) {
			ret.setReturnValue(death);
		}
	}

	@Inject(at = @At("TAIL"), method = "getHurtSound(Lnet/minecraft/entity/damage/DamageSource;)Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getHurtSoundInject(CallbackInfoReturnable<SoundEvent> ret) {
		SoundEvent hurt = voiceManager.getVoiceLine(VillagerVoiceManager.Reason.HURT);
		if(hurt != null) {
			ret.setReturnValue(hurt);
		}
	}

	@Inject(at = @At("TAIL"), method = "levelUp()V")
	private void levelUp(CallbackInfo info) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.LEVEL, this.getSoundVolume());
	}

	@Inject(at = @At("INVOKE"), method = "sayNo()V", cancellable = true)
	private void sayNo(CallbackInfo info) {
		if(voiceManager.speak(world, VillagerVoiceManager.Reason.NO, this.getSoundVolume())) {
			this.setHeadRollingTimeLeft(40);
			info.cancel();
		}
	}

	@Inject(at = @At("TAIL"), method = "eatForBreeding()V")
	public void eatForBreeding(CallbackInfo info) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.EAT, this.getSoundVolume());
	}

	@Inject(at = @At("TAIL"), method = "onGrowUp()V")
	public void onGrowUp(CallbackInfo info) {
		voicePitch = generateVoicePitch();
	}


	@Override // increase default ambient sound delay
	public int getMinAmbientSoundDelay() {
		return 180;
	}

	@Override
	public float getSoundPitch() {
		return voicePitch;
	}
	private float generateVoicePitch() {
		return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
	}

	@Inject(at = @At("TAIL"), method = "writeCustomDataToTag(Lnet/minecraft/nbt/CompoundTag;)V")
	public void writeCustomDataToTag(CompoundTag tag, CallbackInfo ci) {
		tag.putFloat("VoicePitch", this.voicePitch);
		if(this.voiceManager != null && this.voiceManager.getVoice() != null) {
			tag.putString("VoiceName", this.voiceManager.getVoice().getName());
		}
	}

	@Inject(at = @At("TAIL"), method = "readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V")
	public void readCustomDataFromTag(CompoundTag tag, CallbackInfo ci) {
		this.voicePitch = tag.getFloat("VoicePitch");
		if(tag.contains("VoiceName")) {
			String voiceName = tag.getString("VoiceName");
			if(VillagerVoiceManager.containsVoice(voiceName)) {
				this.voiceManager.setVoice(voiceName);
			}
		}
	}

	private boolean isCloseEnough(VillagerEntity villager, PlayerEntity player) {
		BlockPos blockPos = player.getBlockPos();
		BlockPos blockPos2 = villager.getBlockPos();
		return blockPos2.isWithinDistance(blockPos, 5.0D);
	}

	public void sayHero() {
		this.voiceManager.speak(world, VillagerVoiceManager.Reason.HERODROP, this.getSoundVolume());
	}
}

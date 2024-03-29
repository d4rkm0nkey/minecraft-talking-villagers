package me.teajay.talkingvillagers.mixin;

import java.util.Optional;
import java.util.Random;

import me.teajay.talkingvillagers.common.sound.VillagerVoice;
import me.teajay.talkingvillagers.common.sound.VoiceManager;
import me.teajay.talkingvillagers.common.util.IVillagerEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements IVillagerEntity {
	private static final int MIN_RANDOM_COOLDOWN = 300;
	private static final int MAX_RANDOM_COOLDOWN = 5000;
	private static final int MIN_GREETING_COOLDOWN = 6000;
	private static final int MIN_HERO_COOLDOWN = 2000;
	private static final int MIN_EAT_COOLDOWN = 1000;
	private static final int MIN_GOSSIP_COOLDOWN = 50;

	protected Random random = new Random();
	private Float voicePitch;
	private int nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
	private int greetingCoolDown = 0;
	private int heroCoolDown = 0;
	private int eatCooldown = 0;
	private int gossipCooldown = 0;
	@Shadow
	private long gossipStartTime;
	private VillagerVoice voice;

	public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V")
	private void init(CallbackInfo info) {
		voicePitch = generateVoicePitch();
		voice = VoiceManager.getRandomVillagerVoice();
		random = new Random();
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void tick(CallbackInfo info) {
		heroCoolDown--;
		nextRandomTalking--;
		greetingCoolDown--;
		boolean newVoiceEvent = false;
		if(isSleeping()) return;
		if(heroCoolDown > 0) {
			eatCooldown--;
		}
		if(heroCoolDown < 0) {
			Optional<PlayerEntity> heroOpt = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(
					playerEntity -> playerEntity.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)
			);
			if(heroOpt.isPresent()) {
				if(voice.speak(world, VillagerVoiceManager.Reason.HERO, this.getSoundVolume())) {
					newVoiceEvent = true;
				}
				heroCoolDown = MIN_HERO_COOLDOWN;
			}
		}
		if (greetingCoolDown < 0 && !newVoiceEvent) {
			Optional<PlayerEntity> close = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);

			if(close.isPresent() && isCloseEnough(((VillagerEntity) (Object)this), close.get())) {
				if(random.nextInt(50) == 0) {
					if(voice.speak(world, VillagerVoiceManager.Reason.GREETING, this.getSoundVolume())) {
						newVoiceEvent = true;
					}
					greetingCoolDown = MIN_GREETING_COOLDOWN;
				}
			}
		}
		if (nextRandomTalking <= 0 && !newVoiceEvent) {
			if(random.nextInt(150) == 0) {
				boolean randomTalked = false;
				if(random.nextInt(10) == 0) {
					if (world.isRaining()) {
						randomTalked = voiceManager.speak(world, VillagerVoiceManager.Reason.BAD_WEATHER, this.getSoundVolume());
					} else if (!world.isRaining() && !world.isThundering() && world.isDay()) {
						randomTalked = voiceManager.speak(world, VillagerVoiceManager.Reason.GOOD_WEATHER, this.getSoundVolume());
					}
				}
				if(!randomTalked) {
					voice.speak(world, VillagerVoiceManager.Reason.RANDOM, this.getSoundVolume());
				}
				nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "getAmbientSound()Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getAmbientSound(CallbackInfoReturnable<SoundEvent> ret) {
		if(isSleeping()) {
			ret.setReturnValue(null);
			return;
		}
		SoundEvent ambient = voice.getVoiceLine(VillagerVoiceManager.Reason.AMBIENT);
		if(ambient != null) {
			ret.setReturnValue(ambient);
		}
	}

	@Inject(at = @At("TAIL"), method = "getDeathSound()Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getDeathSoundInject(CallbackInfoReturnable<SoundEvent> ret) {
		SoundEvent death = voice.getVoiceLine(VillagerVoiceManager.Reason.DEATH);
		if(death != null) {
			ret.setReturnValue(death);
		}
	}

	@Inject(at = @At("TAIL"), method = "getHurtSound(Lnet/minecraft/entity/damage/DamageSource;)Lnet/minecraft/sound/SoundEvent;", cancellable = true)
	protected void getHurtSoundInject(CallbackInfoReturnable<SoundEvent> ret) {
		SoundEvent hurt = voice.getVoiceLine(VillagerVoiceManager.Reason.HURT);
		if(hurt != null) {
			ret.setReturnValue(hurt);
		}
	}

	@Inject(at = @At("TAIL"), method = "levelUp()V")
	private void levelUp(CallbackInfo info) {
		voice.speak(world, VillagerVoiceManager.Reason.LEVEL, this.getSoundVolume());
	}

	@Inject(at = @At("INVOKE"), method = "sayNo()V", cancellable = true)
	private void sayNo(CallbackInfo info) {
		if(voice.speak(world, VillagerVoiceManager.Reason.NO, this.getSoundVolume())) {
			this.setHeadRollingTimeLeft(40);
			info.cancel();
		}
	}

	@Override
	public SoundEvent getYesSound() {
		SoundEvent yes = voice.getVoiceLine(VillagerVoiceManager.Reason.YES);
		if(yes != null) {
			return yes;
		}
		return super.getYesSound();
	}

	public SoundEvent getNoSound() {
		SoundEvent no = voice.getVoiceLine(VillagerVoiceManager.Reason.NO);
		if (no != null) {
			return no;
		}
		return SoundEvents.ENTITY_VILLAGER_NO;
	}

	@Override
	protected SoundEvent getTradingSound(boolean sold) {
		if(sold) {
			return getYesSound();
		} else {
			return getNoSound();
		}

	}

	@Override
	public void playCelebrateSound() {
		if(!voice.speak(world, VillagerVoiceManager.Reason.CELEBRATE, this.getSoundVolume())) {
			super.playCelebrateSound();
		}
	}

	@Inject(at = @At("TAIL"), method = "onGrowUp()V")
	public void onGrowUp(CallbackInfo info) {
		voicePitch = generateVoicePitch();
	}

	@Inject(at = @At("TAIL"), method = "talkWithVillager(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;J)V")
	public void talkWithVillager(ServerWorld world, VillagerEntity villager, long time, CallbackInfo ci) {
		long startTime2 = ((IVillagerEntity)villager).getGossipStartTime();
		//if ((time < this.gossipStartTime || time >= this.gossipStartTime + 1200L) && (time < startTime2 || time >= startTime2 + 1200L)) {
		voice.speak(world, VillagerVoiceManager.Reason.GOSSIP, this.getSoundVolume());
		//}
	}

	@Override // increase default ambient sound delay
	public int getMinAmbientSoundDelay() {
		return 300;
	}

	@Override
	public float getSoundPitch() {
		return voicePitch;
	}
	private float generateVoicePitch() {
		return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
	}

	@Inject(at = @At("TAIL"), method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void writeCustomDataToNbt(NbtCompound tag, CallbackInfo ci) {
		tag.putFloat("VoicePitch", this.voicePitch);
		if(this.voice != null) {
			tag.putString("VoiceName", this.voice.getName());
		}
	}

	@Inject(at = @At("TAIL"), method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void readCustomDataFromNbt(NbtCompound tag, CallbackInfo ci) {
		this.voicePitch = tag.getFloat("VoicePitch");
		if(this.voicePitch < 0.1f) {
			this.voicePitch = generateVoicePitch();
		}
		if(tag.contains("VoiceName")) {
			String voiceName = tag.getString("VoiceName");
			if(VoiceManager.containsVillagerVoice(voiceName)) {
				this.voice = VoiceManager.getVillagerVoice(voiceName);
			}
		}
	}

	private boolean isCloseEnough(VillagerEntity villager, PlayerEntity player) {
		BlockPos blockPos = player.getBlockPos();
		BlockPos blockPos2 = villager.getBlockPos();
		return blockPos2.isWithinDistance(blockPos, 5.0D);
	}

	public void sayHeroDrop() {
		this.voice.speak(world, VillagerVoiceManager.Reason.HERODROP, this.getSoundVolume());
	}

	public long getGossipStartTime() {
		return gossipStartTime;
	}

	@Inject(at = @At("TAIL"), method = "beginTradeWith(Lnet/minecraft/entity/player/PlayerEntity;)V")
	private void beginTradeWith(PlayerEntity customer, CallbackInfo ci) {
		voice.speak(world, VillagerVoiceManager.Reason.TRADE, this.getSoundVolume());
	}

	@Inject(at = @At("TAIL"), method = "afterUsing(Lnet/minecraft/village/TradeOffer;)V")
	protected void afterUsing(TradeOffer offer, CallbackInfo ci) {
		voice.speak(world, VillagerVoiceManager.Reason.TRADE_SUCCESS, this.getSoundVolume());
	}
}


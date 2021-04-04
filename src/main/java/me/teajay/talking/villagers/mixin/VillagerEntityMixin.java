package me.teajay.talking.villagers.mixin;

import java.util.Optional;
import java.util.Random;

import me.teajay.talking.villagers.common.util.IVillagerEntity;
import net.minecraft.client.sound.Sound;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerGossips;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
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
public abstract class VillagerEntityMixin extends MerchantEntity implements IVillagerEntity {
	@Shadow public abstract VillagerGossips getGossip();

	private static final int MIN_RANDOM_COOLDOWN = 300;
	private static final int MAX_RANDOM_COOLDOWN = 5000;
	private static final int MIN_GREETING_COOLDOWN = 6000;
	private static final int MIN_HERO_COOLDOWN = 2000;

	private VillagerVoiceManager voiceManager;
	protected Random random = new Random();
	private Float voicePitch;
	private int nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
	private int greetingCoolDown = 0;
	private int heroCoolDown = 0;
	@Shadow
	private long gossipStartTime;
	@Shadow
	public VillagerData getVillagerData() {
		throw new AssertionError();
	}

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
		boolean newVoiceEvent = false;
		if(heroCoolDown < 0) {
			Optional<PlayerEntity> heroOpt = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(
					playerEntity -> playerEntity.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)
			);
			if(heroOpt.isPresent()) {
				if(voiceManager.speak(world, VillagerVoiceManager.Reason.HERO, this.getSoundVolume())) {
					newVoiceEvent = true;
				}
				heroCoolDown = MIN_HERO_COOLDOWN;
			}
		}
		if (greetingCoolDown < 0 && !newVoiceEvent) {
			Optional<PlayerEntity> close = this.getBrain().getOptionalMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);

			if(close.isPresent() && isCloseEnough(((VillagerEntity) (Object)this), close.get())) {
				if(random.nextInt(50) == 0) {
					if(voiceManager.speak(world, VillagerVoiceManager.Reason.GREETING, this.getSoundVolume())) {
						newVoiceEvent = true;
					}
					greetingCoolDown = MIN_GREETING_COOLDOWN;
				}
			}
		}
		if (nextRandomTalking <= 0 && !voiceManager.isTalking() && !newVoiceEvent) {
			if(random.nextInt(150) == 0) {
				voiceManager.speak(world, VillagerVoiceManager.Reason.RANDOM, this.getSoundVolume());
				nextRandomTalking = MIN_RANDOM_COOLDOWN + random.nextInt(MAX_RANDOM_COOLDOWN - MIN_RANDOM_COOLDOWN);
			}
		}
		heroCoolDown--;
		nextRandomTalking--;
		greetingCoolDown--;
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

	@Override
	public SoundEvent getYesSound() {
		SoundEvent yes = voiceManager.getVoiceLine(VillagerVoiceManager.Reason.YES);
		if(yes != null) {
			return yes;
		}
		return super.getYesSound();
	}

	public SoundEvent getNoSound() {
		SoundEvent no = voiceManager.getVoiceLine(VillagerVoiceManager.Reason.NO);
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
		if(!voiceManager.speak(world, VillagerVoiceManager.Reason.CELEBRATE, this.getSoundVolume())) {
			super.playCelebrateSound();
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

	@Inject(at = @At("TAIL"), method = "talkWithVillager(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;J)V")
	public void talkWithVillager(ServerWorld world, VillagerEntity villager, long time, CallbackInfo ci) {
		long startTime2 = ((IVillagerEntity)villager).getGossipStartTime();
		if ((time < this.gossipStartTime || time >= this.gossipStartTime + 1200L) && (time < startTime2 || time >= startTime2 + 1200L)) {
			voiceManager.speak(world, VillagerVoiceManager.Reason.GOSSIP, this.getSoundVolume());
		}
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

	public void sayHeroDrop() {
		this.voiceManager.speak(world, VillagerVoiceManager.Reason.HERODROP, this.getSoundVolume());
	}

	public long getGossipStartTime() {
		return gossipStartTime;
	}

	@Inject(at = @At("TAIL"), method = "beginTradeWith(Lnet/minecraft/entity/player/PlayerEntity;)V")
	private void beginTradeWith(PlayerEntity customer, CallbackInfo ci) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.TRADE, this.getSoundVolume());
	}

	@Inject(at = @At("TAIL"), method = "afterUsing(Lnet/minecraft/village/TradeOffer;)V")
	protected void afterUsing(TradeOffer offer, CallbackInfo ci) {
		voiceManager.speak(world, VillagerVoiceManager.Reason.TRADE_SUCCESS, this.getSoundVolume());
	}
}

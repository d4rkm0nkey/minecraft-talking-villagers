package me.teajay.talking.villagers.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
	public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
	}

	private VillagerVoiceManager voiceManager;
	protected Random random;

	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V")
	private void init(CallbackInfo info) {
		voiceManager = new VillagerVoiceManager();
		random = new Random();
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void tick(CallbackInfo info) {
		if(random.nextInt(100) == 0) {
			voiceManager.speak(world, VillagerVoiceManager.Reason.RANDOM, this.getBlockPos());
		}
	}
}

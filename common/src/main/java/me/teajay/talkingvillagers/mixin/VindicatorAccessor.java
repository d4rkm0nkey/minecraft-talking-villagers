package me.teajay.talkingvillagers.mixin;

import net.minecraft.world.entity.monster.illager.Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Vindicator.class)
public interface VindicatorAccessor {
    @Accessor("isJohnny")
    boolean talkingvillagers$isJohnny();
}

package me.teajay.talkingvillagers.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * A voice line that follows the speaking entity. It builds its {@link Sound} itself instead of looking up a
 * {@code sounds.json} entry, because voice lines come from voicepacks, not resource packs.
 */
public class VoiceSoundInstance extends AbstractTickableSoundInstance {
    private static final int ATTENUATION_DISTANCE = 16;
    private final Entity entity;
    private final @Nullable Component subtitle;

    public VoiceSoundInstance(Identifier location, Entity entity, float volume, float pitch, @Nullable Component subtitle) {
        super(SoundEvent.createVariableRangeEvent(location), entity.getSoundSource(), RandomSource.create());
        this.entity = entity;
        this.volume = volume;
        this.pitch = pitch;
        this.subtitle = subtitle;
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
        this.sound = new Sound(identifier, ConstantFloat.of(1.0f), ConstantFloat.of(1.0f), 1, Sound.Type.FILE, false, false, ATTENUATION_DISTANCE);
        this.soundEvent = new SubtitledEvents(identifier, subtitle);
        return soundEvent;
    }

    @Override
    public boolean canPlaySound() {
        return !entity.isSilent();
    }

    @Override
    public void tick() {
        if (entity.isRemoved()) {
            stop();
        } else {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }
    }

    private static final class SubtitledEvents extends WeighedSoundEvents {
        private final @Nullable Component subtitle;

        SubtitledEvents(Identifier location, @Nullable Component subtitle) {
            super(location, null);
            this.subtitle = subtitle;
        }

        @Override
        public @Nullable Component getSubtitle() {
            return subtitle;
        }
    }
}

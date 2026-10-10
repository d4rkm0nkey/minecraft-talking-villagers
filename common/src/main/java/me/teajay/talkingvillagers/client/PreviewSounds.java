package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.jspecify.annotations.Nullable;

/**
 * Plays voice lines in menus, without an entity and not positional. Only one preview plays at a time.
 */
public final class PreviewSounds {
    private static @Nullable SoundInstance current;

    private PreviewSounds() {
    }

    public static void play(LoadedPack pack, VoiceLine line, float pitch) {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        stop();
        current = new Preview(VoiceAudio.soundLocation(pack.sha256(), line.file()), Configs.client().volume(), pitch);
        sounds.play(current);
    }

    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    private static final class Preview extends AbstractSoundInstance {
        Preview(Identifier location, float volume, float pitch) {
            super(location, SoundSource.VOICE, RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
            this.sound = new Sound(identifier, ConstantFloat.of(1.0f), ConstantFloat.of(1.0f), 1, Sound.Type.FILE, false, false, 16);
            this.soundEvent = new WeighedSoundEvents(identifier, null);
            return soundEvent;
        }
    }
}

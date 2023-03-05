package me.teajay.talkingvillagers.gui;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.system.CallbackI;

public class MenuSounds {
    public static final Identifier INVALID_VOICEPACK = new Identifier("talkingvillagers:invalid_voicepack");
    public static final Identifier VOICEPACK_INSTALLED  = new Identifier("talkingvillagers:voicepack_installed");
    public static final Identifier VOICEPACK_UNINSTALLED = new Identifier("talkingvillagers:voicepack_uninstalled");
    public static final Identifier VOICE_INSTALLED = new Identifier("talkingvillagers:voice_installed");
    public static final Identifier VOICE_UNINSTALLED = new Identifier("talkingvillagers:voice_uninstalled");
    public static final Identifier UNINSTALL_ERROR = new Identifier("talkingvillagers:uninstall_error");

    public static SoundEvent INVALID_VOICEPACK_EVENT = SoundEvent.of(INVALID_VOICEPACK);
    public static SoundEvent VOICEPACK_INSTALLED_EVENT = SoundEvent.of(VOICEPACK_INSTALLED);
    public static SoundEvent VOICEPACK_UNINSTALLED_EVENT = SoundEvent.of(VOICEPACK_UNINSTALLED);
    public static SoundEvent VOICE_INSTALLED_EVENT = SoundEvent.of(VOICE_INSTALLED);
    public static SoundEvent VOICE_UNINSTALLED_EVENT = SoundEvent.of(VOICE_UNINSTALLED);
    public static final SoundEvent UNINSTALL_ERROR_EVENT = SoundEvent.of(UNINSTALL_ERROR);

    public static void initializeSounds() {
        Registry.register(Registries.SOUND_EVENT, INVALID_VOICEPACK, INVALID_VOICEPACK_EVENT);
        Registry.register(Registries.SOUND_EVENT, VOICEPACK_INSTALLED, VOICEPACK_INSTALLED_EVENT);
        Registry.register(Registries.SOUND_EVENT, VOICEPACK_UNINSTALLED, VOICEPACK_UNINSTALLED_EVENT);
        Registry.register(Registries.SOUND_EVENT, VOICE_INSTALLED, VOICE_INSTALLED_EVENT);
        Registry.register(Registries.SOUND_EVENT, VOICE_UNINSTALLED, VOICE_UNINSTALLED_EVENT);
        Registry.register(Registries.SOUND_EVENT, UNINSTALL_ERROR, UNINSTALL_ERROR_EVENT);
    }
}

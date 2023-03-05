package me.teajay.talkingvillagers.voicepacks;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceConfig.VoiceConfig;
import me.teajay.talkingvillagers.io.VoiceConfigSaver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages which voicepacks and voices are installed
 */
public class VoicepackManager {

    public static ArrayList<Voicepack> getVoicePacks() {
        final var voicepacks = new ArrayList<Voicepack>();

        for (File resourcepack : TalkingVillagers.VOICEPACK_DIRECTORY.toFile().listFiles(path -> path.getPath().endsWith(".zip"))) {
            var reader = new VoicepackReader(resourcepack);
            if (reader.hasVoicepack()) {
                voicepacks.add(reader.getVoicepack());
            }
        }
        return voicepacks;
    }

    public static boolean installVoicepack(Voicepack voicepack) {
        boolean success = true;
        for (var voice : voicepack.getVoices()) {
            if(!voice.isInstalled()) {
                success &= installVoice(voicepack.getSounds(), voice);
            }
        }
        return success;
    }

    public static boolean uninstallVoicepack(Voicepack voicepack) {
        boolean success = true;
        for (var voice : voicepack.getVoices()) {
            if(voice.isInstalled()) {
                success &= uninstallVoice(voice);
            }
        }
        return success;
    }

    public static boolean uninstallVoice(VoiceFile voice) {
        try {
            File voiceFile = Path.of(TalkingVillagers.CONFIG_DIRECTORY.toString(), "voices", voice.getId() + ".json").toFile();
            TalkingVillagers.LOGGER.debug("try to delete: " + voiceFile.getAbsolutePath());
            Files.delete(voiceFile.toPath());
            return true;
        } catch (IOException e) {
            TalkingVillagers.LOGGER.error(e);
            return false;
        }
    }

    public static boolean installVoice(List<SoundEntry> sounds, VoiceFile voice) {
        var voiceConfig = voice.toVoiceConfig(sounds);
        if(voiceConfig == null) {
            TalkingVillagers.LOGGER.error("Error installing voice " + voice.getId() + ". Invalid VoicePack.", VoicepackManager.class);
            return false;
        }
        return VoiceConfigSaver.saveVoiceConfig(voiceConfig);
    }
}

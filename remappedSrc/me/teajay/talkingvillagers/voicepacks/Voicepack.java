package me.teajay.talkingvillagers.voicepacks;

import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceConfig.VoiceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a voice resourcepack
 */
public class Voicepack {
    private final String id;
    private final String name;
    private final String version;

    private List<VoiceFile> voices = new ArrayList<>();
    private List<SoundEntry> sounds = new ArrayList<>();

    public Voicepack(String id, String name, String version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void addVoice(VoiceFile voice) {
        voices.add(voice);
    }

    public List<VoiceFile> getVoices() {
        return voices;
    }

    public List<SoundEntry> getSounds() {
        return sounds;
    }

    public void setSounds(List<SoundEntry> sounds) {
        this.sounds = sounds;
    }

    public List<VoiceConfig> toVoiceConfigs() {
        var configs = new ArrayList<VoiceConfig>();
        for (var sound : sounds) {
            String voiceId = sound.getVoiceId();
            VoiceConfig config = getVoice(configs, voiceId);
            if(config == null) {
                config = VoiceConfig.Create(sound, voiceId);
                configs.add(config);
            }
            config.addSound(sound);
        }
        return configs;
    }

    private VoiceConfig getVoice(ArrayList<VoiceConfig> configs, String voiceId) {
        for (var config : configs) {
            if(config.getId() == voiceId) {
                return config;
            }
        }
        return null;
    }

    public boolean isFullyInstalled() {
        return this.getVoices().stream().allMatch(v -> v.isInstalled());
    }
}

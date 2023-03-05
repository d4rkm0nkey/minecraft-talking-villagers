package me.teajay.talkingvillagers.voicepacks;

import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceConfig.VoiceConfig;
import me.teajay.talkingvillagers.io.VoiceInfo;

import java.util.List;

/**
 * Represents the voices inside a voice resourcepack
 */
public class VoiceFile {
    private final String id;
    private final String name;
    private final VoiceInfo.VoiceType type;
    private boolean isInstalled;

    public VoiceFile(String id, String name, VoiceInfo.VoiceType type, boolean isInstalled) {
        this.id = id;
        this.type = type;
        this.isInstalled = isInstalled;
        this.name = name;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VoiceInfo.VoiceType getType() {
        return type;
    }

    public VoiceConfig toVoiceConfig(List<SoundEntry> sounds) {
        VoiceConfig config = null;
        for (var sound : sounds) {
            if(sound.getVoiceId().equals(id)) {
                if(config == null) {
                    config = VoiceConfig.Create(sound, sound.getVoiceId());
                }
                config.addSound(sound);
            }
        }
        return config;
    }
}

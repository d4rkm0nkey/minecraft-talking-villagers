package me.teajay.talkingvillagers.io.VoiceConfig;

import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceInfo;

import java.util.Locale;

public abstract class VoiceConfig {
    protected String id;
    protected String voicepack_id;
    protected VoiceInfo.VoiceType type;


    public VoiceConfig(SoundEntry sound, String voicepackId) {
        id = sound.getVoiceId();
        voicepack_id = voicepackId;
        this.addSound(sound);
    }

    public static VoiceConfig Create(SoundEntry sound, String voicepack_id) {
        return switch (sound.getType().toUpperCase(Locale.ROOT)) {
            case "VILLAGER" -> new VillagerVoiceConfig(sound, voicepack_id);
            case "PILLAGER" -> new PillagerVoiceConfig(sound, voicepack_id);
            case "WITCH" -> new WitchVoiceConfig(sound, voicepack_id);
            case "WANDERING_TRADER" -> new WanderingTraderVoiceConfig(sound, voicepack_id);
            default -> throw new RuntimeException("Unknown voice type " + sound.getType());
        };
    }

    public abstract String toJsonString();

    public String getId() {
        return id;
    }

    public String getVoicepack_id() {
        return voicepack_id;
    }

    public VoiceInfo.VoiceType getType() {
        return type;
    }

    public abstract void addSound(SoundEntry sound);
}

package me.teajay.talkingvillagers.io.VoiceConfig;

import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceInfo;

public class PillagerVoiceConfig extends ProfessionVoiceConfig {
    public PillagerVoiceConfig(SoundEntry sound, String voicepackId) {
        super(sound, voicepackId);
        type = VoiceInfo.VoiceType.PILLAGER;
    }
}

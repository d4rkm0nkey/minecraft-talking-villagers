package me.teajay.talkingvillagers.io.VoiceConfig;

import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceInfo;

public class WanderingTraderVoiceConfig extends SimpleVoiceConfig {
    public WanderingTraderVoiceConfig(SoundEntry sound, String voicepackId) {
        super(sound, voicepackId);
        type = VoiceInfo.VoiceType.WANDERING_TRADER;
    }
}

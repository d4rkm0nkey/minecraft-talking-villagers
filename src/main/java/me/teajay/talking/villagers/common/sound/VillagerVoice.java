package me.teajay.talking.villagers.common.sound;

import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.common.data.VoiceData;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VillagerVoice {
    private final String name;
    private Random random = new Random();
    private Map<VillagerVoiceManager.Reason, Map<Identifier, SoundEvent>> voiceLines = new HashMap<>();

    public VillagerVoice(String name) {
        this.name = name;
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            voiceLines.put(reason, new HashMap<>());
        }
    }

    public void loadData(VoiceData data) {
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            for (String voiceLine : data.getVoiceLines(reason)) {
                Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.name + "-" + voiceLine);
                SoundEvent sound_event = new SoundEvent(sound_id);
                voiceLines.get(reason).put(sound_id, sound_event);
                Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
            }
        }
    }


    public SoundEvent getVoiceLine(VillagerVoiceManager.Reason reason) {
        Map<Identifier, SoundEvent> events = voiceLines.get(reason);
        if(events.keySet().size() > 0)
            return events.get(events.keySet().toArray()[random.nextInt(events.keySet().size())]);
        else
            return null;
    }
}

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
    private String name;
    private final String identifier;
    private final Random random = new Random();
    private final Map<VillagerVoiceManager.Reason, Map<Identifier, SoundEvent>> voiceLines = new HashMap<>();

    public VillagerVoice(String identifier) {
        this.identifier = identifier;
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            voiceLines.put(reason, new HashMap<>());
        }
    }

    public void loadData(VoiceData data) {
        name = data.getName();
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            for (String voiceLine : data.getVoiceLines(reason)) {
                Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.identifier + "-" + voiceLine);
                SoundEvent sound_event = new SoundEvent(sound_id);
                voiceLines.get(reason).put(sound_id, sound_event);
                Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
            }
        }
    }


    public SoundEvent getVoiceLine(VillagerVoiceManager.Reason reason) {
        Map<Identifier, SoundEvent> events = voiceLines.get(reason);
        if(events.keySet().size() > 0) {
            Identifier id = (Identifier) events.keySet().toArray()[random.nextInt(events.keySet().size())];
            return events.get(id);
        }
        return null;
    }

    public String getName() {
        return name;
    }
}

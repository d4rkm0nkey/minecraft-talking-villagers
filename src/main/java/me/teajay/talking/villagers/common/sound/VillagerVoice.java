package me.teajay.talking.villagers.common.sound;

import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.common.data.VoiceDataContainer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VillagerVoice {
    private String name;
    private final String identifier;
    private final Random random = new Random();
    private final Map<VillagerVoiceManager.Reason, Map<Identifier, SoundEvent>> voiceLines = new HashMap<>();
    private final Map<VoiceDataContainer.Profession, Map<VillagerVoiceManager.Reason, Map<Identifier, SoundEvent>>> professionVoiceLines = new HashMap<>();

    public VillagerVoice(String identifier) {
        this.identifier = identifier;
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            voiceLines.put(reason, new HashMap<>());
        }
        for(VoiceDataContainer.Profession prof : VoiceDataContainer.Profession.values()) {
            Map<VillagerVoiceManager.Reason, Map<Identifier, SoundEvent>> professionMap = new HashMap<>();
            professionVoiceLines.put(prof, professionMap);
            for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
                professionMap.put(reason, new HashMap<>());
            }
        }
    }

    public void loadData(VoiceDataContainer data) {
        name = data.getName();
        for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
            for (String voiceLine : data.getDefaultVoiceLines(reason)) {
                Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.identifier + "-" + voiceLine);
                SoundEvent sound_event = new SoundEvent(sound_id);
                voiceLines.get(reason).put(sound_id, sound_event);
                Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
            }
        }
        for (VoiceDataContainer.Profession profession : VoiceDataContainer.Profession.values()) {
            for(VillagerVoiceManager.Reason reason : VillagerVoiceManager.Reason.values()) {
                for (String voiceLine : data.getProfessionVoiceLines(reason, profession)) {
                    Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.identifier + "-" + voiceLine);
                    if(Registry.SOUND_EVENT.getIds().contains(sound_id)) {
                        professionVoiceLines.get(profession).get(reason).put(sound_id, Registry.SOUND_EVENT.get(sound_id));
                    } else {
                        SoundEvent sound_event = new SoundEvent(sound_id);
                        professionVoiceLines.get(profession).get(reason).put(sound_id, sound_event);
                        Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
                    }
                }
            }
        }
    }


    private SoundEvent getVoiceLine(VillagerVoiceManager.Reason reason) {
        Map<Identifier, SoundEvent> events = voiceLines.get(reason);
        if(events.size() > 0) {
            Identifier id = (Identifier) events.keySet().toArray()[random.nextInt(events.size())];
            return events.get(id);
        }
        return null;
    }

    public SoundEvent getVoiceLine(VillagerVoiceManager.Reason reason, VillagerProfession profession) {
        Map<Identifier, SoundEvent> events = new HashMap<>();
        if(random.nextInt(2) == 0) {
            for(VoiceDataContainer.Profession prof : VoiceDataContainer.Profession.values()) {
                if(prof.getProfession() == profession) {
                    events = professionVoiceLines.get(prof).get(reason);
                    break;
                }
            }
        }
        if(events.size() < 1) {
            return getVoiceLine(reason);
        } else {
            Identifier id = (Identifier) events.keySet().toArray()[random.nextInt(events.size())];
            return events.get(id);
        }
    }

    public String getName() {
        return name;
    }
}

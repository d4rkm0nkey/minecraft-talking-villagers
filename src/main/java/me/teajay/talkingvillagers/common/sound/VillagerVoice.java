package me.teajay.talkingvillagers.common.sound;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.common.data.VoiceDataContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import java.util.HashMap;
import java.util.Map;

public class VillagerVoice extends EntityVoice {
    private final Map<VoiceDataContainer.Profession, Map<Reason, Map<Identifier, SoundEvent>>> professionVoiceLines = new HashMap<>();

    public VillagerVoice(String identifier) {
        super(identifier);
        for(VoiceDataContainer.Profession prof : VoiceDataContainer.Profession.values()) {
            Map<Reason, Map<Identifier, SoundEvent>> professionMap = new HashMap<>();
            professionVoiceLines.put(prof, professionMap);
            for(Reason reason : Reason.values()) {
                professionMap.put(reason, new HashMap<>());
            }
        }
    }

    @Override
    public void loadData(VoiceDataContainer data) {
        super.loadData(data);
        for (VoiceDataContainer.Profession profession : VoiceDataContainer.Profession.values()) {
            for(Reason reason : Reason.values()) {
                for (String voiceLine : data.getProfessionVoiceLines(reason, profession)) {
                    Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.identifier + "-" + voiceLine);
                    if(Registries.SOUND_EVENT.getIds().contains(sound_id)) {
                        professionVoiceLines.get(profession).get(reason).put(sound_id, Registries.SOUND_EVENT.get(sound_id));
                    } else {
                        SoundEvent sound_event = SoundEvent.of(sound_id);
                        professionVoiceLines.get(profession).get(reason).put(sound_id, sound_event);
                        Registry.register(Registries.SOUND_EVENT, sound_id, sound_event);
                    }
                }
            }
        }
    }

    public SoundEvent getVoiceLine(Reason reason, VillagerProfession profession) {
        Map<Identifier, SoundEvent> events = new HashMap<>();
        if (random.nextInt(2) == 0) {
            for (VoiceDataContainer.Profession prof : VoiceDataContainer.Profession.values()) {
                if (prof.getProfession() == profession) {
                    events = professionVoiceLines.get(prof).get(reason);
                    break;
                }
            }
        }
        if (events.size() < 1) {
            return getVoiceLine(reason);
        } else {
            Identifier id = (Identifier) events.keySet().toArray()[random.nextInt(events.size())];
            return events.get(id);
        }
    }
}

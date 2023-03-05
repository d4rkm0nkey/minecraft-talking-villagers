package me.teajay.talkingvillagers.common.sound;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.common.data.VoiceDataContainer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

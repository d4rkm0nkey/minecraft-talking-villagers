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
    private Map<Identifier, SoundEvent> randomVoiceLines = new HashMap<Identifier, SoundEvent>();
    private Map<Identifier, SoundEvent> deathVoiceLines = new HashMap<Identifier, SoundEvent>();
    private Map<Identifier, SoundEvent> tradeVoiceLines = new HashMap<Identifier, SoundEvent>();
    private Map<Identifier, SoundEvent> gossipVoiceLines = new HashMap<Identifier, SoundEvent>();

    public VillagerVoice(String name) {
        this.name = name;
    }

    public void loadData(VoiceData data) {
        for (String voiceLine : data.getDeath()) {
            Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.name + "-" + voiceLine);
            SoundEvent sound_event = new SoundEvent(sound_id);
            deathVoiceLines.put(sound_id, sound_event);
            Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
        }
        for (String voiceLine : data.getGossip()) {
            Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.name + "-" + voiceLine);
            SoundEvent sound_event = new SoundEvent(sound_id);
            gossipVoiceLines.put(sound_id, sound_event);
            Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
        }
        for (String voiceLine : data.getRandom()) {
            Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.name + "-" + voiceLine);
            SoundEvent sound_event = new SoundEvent(sound_id);
            randomVoiceLines.put(sound_id, sound_event);
            Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
        }
        for (String voiceLine : data.getTrade()) {
            Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.name + "-" + voiceLine);
            SoundEvent sound_event = new SoundEvent(sound_id);
            tradeVoiceLines.put(sound_id, sound_event);
            Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
        }
    }


    public SoundEvent getVoiceLine(VillagerVoiceManager.Reason reason) {
        switch (reason) {
            case DEATH:
                if(deathVoiceLines.keySet().size() > 0)
                return deathVoiceLines.get(deathVoiceLines.keySet().toArray()[random.nextInt(deathVoiceLines.keySet().size())]);
                else return null;
            case TRADE:
                if(tradeVoiceLines.keySet().size() > 0)
                return tradeVoiceLines.get(tradeVoiceLines.keySet().toArray()[random.nextInt(tradeVoiceLines.keySet().size())]);
                else return null;
            case GOSSIP:
                if(gossipVoiceLines.keySet().size() > 0)
                return gossipVoiceLines.get(gossipVoiceLines.keySet().toArray()[random.nextInt(gossipVoiceLines.keySet().size())]);
                else return null;
            case RANDOM:
                if(randomVoiceLines.keySet().size() > 0)
                return randomVoiceLines.get(randomVoiceLines.keySet().toArray()[random.nextInt(randomVoiceLines.keySet().size())]);
                else return null;
        }
        return null;
    }
}

package me.teajay.talkingvillagers.common.sound;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.common.data.VoiceDataContainer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class EntityVoice {
    public enum Reason {
        DEATH,
        RANDOM,
        TRADE,
        TRADE_SUCCESS,
        GOSSIP,
        HURT,
        YES,
        NO,
        LEVEL,
        HERO,
        HERODROP,
        AMBIENT,
        GREETING,
        CELEBRATE,
        GOOD_WEATHER,
        BAD_WEATHER
    }
    protected final String identifier;
    protected String name;
    protected final Map<Reason, Map<Identifier, SoundEvent>> voiceLines = new HashMap<>();
    protected final Random random = new Random();

    public EntityVoice(String identifier) {
        this.identifier = identifier;
        for(Reason reason : Reason.values()) {
            voiceLines.put(reason, new HashMap<>());
        }
    }

    public void loadData(VoiceDataContainer data) {
        name = data.getId();
        for(Reason reason : Reason.values()) {
            for (String voiceLine : data.getDefaultVoiceLines(reason)) {
                Identifier sound_id = new Identifier(TalkingVillagers.MODID, this.identifier + "-" + voiceLine);
                SoundEvent sound_event = new SoundEvent(sound_id);
                voiceLines.get(reason).put(sound_id, sound_event);
                Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
            }
        }
    }

    public static EntityVoice create(String identifier) {
        throw new NotImplementedException();
    }

    public SoundEvent getVoiceLine(Reason reason) {
        Map<Identifier, SoundEvent> events = voiceLines.get(reason);
        if(events.size() > 0) {
            Identifier id = (Identifier) events.keySet().toArray()[random.nextInt(events.size())];
            return events.get(id);
        }
        return null;
    }

    public Boolean speak(World world, Reason reason, float volume, MobEntity entity) {
        if (!world.isClient) {
            SoundEvent event = getVoiceLine(reason);
            if(event != null) {
                entity.playSound(event, volume, entity.getSoundPitch());
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }
}

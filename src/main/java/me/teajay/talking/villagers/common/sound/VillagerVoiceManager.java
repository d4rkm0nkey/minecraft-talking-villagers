package me.teajay.talking.villagers.common.sound;

import java.io.File;
import java.util.HashMap;
import java.util.Random;

import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.common.data.VoiceData;
import me.teajay.talking.villagers.common.util.JsonLoader;
import me.teajay.talking.villagers.common.util.IVillagerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VillagerVoiceManager {
    public enum Reason {
        DEATH,
        RANDOM,
        TRADE,
        GOSSIP,
        HURT,
        EAT,
        YES,
        NO,
        LEVEL,
        HERO,
        HERODROP,
        AMBIENT,
        GREETING
    }
    private static final Random random = new Random();
    private static final HashMap<String, VillagerVoice> voices = new HashMap<>();
    @Nullable
    private VillagerVoice voice;
    private final VillagerEntity villager;
    // ToDo At waiting queue for voice events if queue > 5 queue is reset to 0
    // ToDo greetings

    public static void initialiseVoices() {
        File voiceDir = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID + "/voices");
        if (!voiceDir.exists()) {
            if(!voiceDir.mkdirs()) {
                System.err.println("Couldn't create voice folder.");
            }
        }
        for(File voiceConfig : voiceDir.listFiles(path -> path.getPath().endsWith(".json"))) {
            VoiceData data = JsonLoader.parseFromFile(voiceConfig);
            if (data != null) {
                String identifier = voiceConfig.getName().replace(".json", "");
                VillagerVoice voice = new VillagerVoice(identifier);
                voice.loadData(data);
                voices.put(identifier, voice);
            }
        }
    }

    public VillagerVoiceManager(VillagerEntity villager) {
        this.villager = villager;
        this.voice = getRandomVoice();
    }

    public Boolean speak(World world, Reason reason, float volume) {
        if (!world.isClient && voice != null) {
            SoundEvent event = voice.getVoiceLine(reason);
            if(event != null)
                villager.playSound(event, volume, ((IVillagerEntity)villager).getSoundPitch());
                return true;
        }
        return false;
    }

    public static VillagerVoice getRandomVoice() {
        if(voices.size() > 0) {
            return (VillagerVoice) voices.values().toArray()[random.nextInt(voices.size())];
        } else {
            return null;
        }
    }

    public static Boolean containsVoice(String identifier) {
        return voices.containsKey(identifier);
    }

    public static VillagerVoice getVoice(String identifier) {
        if(containsVoice(identifier))
            return voices.get(identifier);
        else
            return null;
    }

    @Nullable
    public VillagerVoice getVoice() {
        return voice;
    }

    public SoundEvent getVoiceLine (Reason reason) {
        if(voice != null)
            return voice.getVoiceLine(reason);
        else
            return null;
    }

    public void setVoice(String voiceName) {
        this.voice = VillagerVoiceManager.getVoice(voiceName);
    }

    //ToDo
    public Boolean isTalking() {
        return false;
    }
}
package me.teajay.talking.villagers.common.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.common.data.VoiceData;
import me.teajay.talking.villagers.common.util.JsonLoader;
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
        NO,
        LEVEL,
        HERO
    }
    private static final Random random = new Random();
    private static final ArrayList<VillagerVoice> voices = new ArrayList<>();
    @Nullable
    private final VillagerVoice voice;
    private final VillagerEntity villager;
    // ToDo At waiting queue for voice events if queue > 5 queue is reset to 0

    public static void initialiseVoices() {
        File voiceDir = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID + "/voices");
        if (!voiceDir.exists()) {
            voiceDir.mkdirs();
        }
        for(File voiceConfig : voiceDir.listFiles(path -> path.getPath().endsWith(".json"))) {
            VoiceData data = JsonLoader.parseFromFile(voiceConfig);
            if (data != null) {
                VillagerVoice voice = new VillagerVoice(voiceConfig.getName().replace(".json", ""));
                voice.loadData(data);
                voices.add(voice);
            }
        }
    }

    public VillagerVoiceManager(VillagerEntity villager) {
        this.villager = villager;
        this.voice = getRandomVoice();
    }
    public void speak(World world, Reason reason) {
        speak(world, reason, 1.0f, 1.0f);
    }

    public void speak(World world, Reason reason, float volume, float pitch) {
        if (!world.isClient && voice != null) {
            SoundEvent event = voice.getVoiceLine(reason);
            if(event != null)
                villager.playSound(event, volume, pitch);
        }
    }

    public static VillagerVoice getRandomVoice() {
        if(voices.size() > 0) {
            return voices.get(random.nextInt(voices.size()));
        } else {
            return null;
        }
    }

    public VillagerVoice getVoice() {
        return voice;
    }
}
package me.teajay.talkingvillagers.common.sound;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.common.data.VoiceDataContainer;
import me.teajay.talkingvillagers.common.util.JsonLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Random;

public class VoiceManager {
    private static final Random random = new Random();
    protected static final HashMap<String, VillagerVoice> villagerVoices = new HashMap<>();
    protected static final HashMap<String, WitchVoice> witchVoices = new HashMap<>();
    protected static final HashMap<String, EntityVoice> pillagerVoices = new HashMap<>();
    protected static final HashMap<String, EntityVoice> wanderingTraderVoices = new HashMap<>();

    public static void initialiseVoices() {
        File voiceDir = new File(TalkingVillagers.CONFIG_DIRECTORY + "/voices");
        for(File voiceConfig : voiceDir.listFiles(path -> path.getPath().endsWith(".json"))) {
            VoiceDataContainer data = JsonLoader.parseFromFile(voiceConfig);
            if(data == null) continue;
            String identifier = voiceConfig.getName().replace(".json", "");
            switch (VoiceDataContainer.Type.fromString(data.getType())) {
                case Villager:
                    VillagerVoice voice = new VillagerVoice(identifier);
                    voice.loadData(data);
                    villagerVoices.put(identifier, voice);
                    break;
                case Witch:
                    WitchVoice witchVoice =  new WitchVoice(identifier);
                    witchVoice.loadData(data);
                    witchVoices.put(identifier, witchVoice);
                    break;
                case Pillager:
                    break;
                case WanderingTrader:
                    break;
            }
        }
    }

    public static VillagerVoice getRandomVillagerVoice() {
        if(villagerVoices.size() > 0) {
            return (VillagerVoice) villagerVoices.values().toArray()[random.nextInt(villagerVoices.size())];
        } else {
            return null;
        }
    }

    public static Boolean containsVillagerVoice(String identifier) {
        return villagerVoices.containsKey(identifier);
    }

    public static VillagerVoice getVillagerVoice(String identifier) {
        if(containsVillagerVoice(identifier))
            return villagerVoices.get(identifier);
        else
            return null;
    }

    public static WitchVoice getRandomWitchVoice() {
        if(witchVoices.size() > 0) {
            return (WitchVoice) witchVoices.values().toArray()[random.nextInt(witchVoices.size())];
        } else {
            return null;
        }
    }
}

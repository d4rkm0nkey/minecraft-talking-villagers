package me.teajay.talking.villagers.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.common.data.DefaultVoice.DefaultVoice;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;

public class ConfigLoader {
    public static Config loadConfig() {
        Config config = Config.createDefaultConfig();
        File configDir = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID);
        if (!configDir.exists()) {
            if(!configDir.mkdirs()) {
                System.err.println("Couldn't create config folder.");
            }
        }
        File configFile = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID + "/config.json");
        Gson gson;
        try {
            if (configFile.exists()){
                gson = new Gson();
                Reader reader = Files.newBufferedReader(configFile.toPath());
                config = gson.fromJson(reader, Config.class);
            } else {
                gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = Files.newBufferedWriter(configFile.toPath());
                config = Config.createDefaultConfig();
                gson.toJson(config, writer);
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Default config can not be created.");
            e.printStackTrace();
        }
        File voiceDir = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID + "/voices");
        if (!voiceDir.exists()) {
            if(!voiceDir.mkdirs()) {
                System.err.println("Couldn't create voice folder.");
            }
        }
        if(config.isCreateDefaultVoice()) {
            File defaultVoice = new File(voiceDir, "teajay.json");
            if(!defaultVoice.exists()) {
                gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = null;
                try {
                    writer = Files.newBufferedWriter(defaultVoice.toPath());
                    DefaultVoice defaultVoiceConfig = DefaultVoice.createDefaultVoice();
                    gson.toJson(defaultVoiceConfig, writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return config;
    }
}

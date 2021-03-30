package me.teajay.talking.villagers.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import me.teajay.talking.villagers.common.data.VoiceData;
import me.teajay.talking.villagers.common.sound.VillagerVoice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonLoader {
    public static VoiceData parseFromFile(File file) {
        Gson gson = new Gson();
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            VoiceData data = gson.fromJson(reader, VoiceData.class);
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

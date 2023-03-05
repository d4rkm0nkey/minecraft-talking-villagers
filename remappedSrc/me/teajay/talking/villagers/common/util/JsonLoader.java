package me.teajay.talking.villagers.common.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import me.teajay.talking.villagers.common.data.VoiceDataContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonLoader {
    public static VoiceDataContainer parseFromFile(File file) {
        Gson gson = new Gson();
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            return gson.fromJson(reader, VoiceDataContainer.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

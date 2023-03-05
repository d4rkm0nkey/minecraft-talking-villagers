package me.teajay.talking.villagers.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Config {
    private boolean createDefaultVoice = true;

    public Config(boolean createDefaultVoice) {
        this.createDefaultVoice = createDefaultVoice;
    }

    private Config() {
    }

    public static Config createDefaultConfig() {
        return new Config();
    }

    public boolean isCreateDefaultVoice() {
        return createDefaultVoice;
    }
}

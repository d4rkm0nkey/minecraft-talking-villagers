package me.teajay.talkingvillagers.io;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.io.VoiceConfig.VoiceConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class VoiceConfigSaver {
    private static FileWriter file;
    public static boolean saveVoiceConfig(VoiceConfig config) {
        var path = Path.of(TalkingVillagers.CONFIG_DIRECTORY.toString(), "voices", config.getId() + ".json");
        try {
            file = new FileWriter(path.toString());
            String out = config.toJsonString();
            file.write(out);
        } catch (IOException e) {
            TalkingVillagers.LOGGER.error(e);
            return false;
        } finally {
            try {
                file.flush();
                file.close();
                return true;
            } catch (IOException e) {
                TalkingVillagers.LOGGER.error(e);
                return false;
            }
        }
    }
}

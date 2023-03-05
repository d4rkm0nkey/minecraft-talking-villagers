package me.teajay.talkingvillagers.io.VoiceConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceProfession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfessionVoiceConfig extends VoiceConfig {
    private ArrayList<VoiceProfession> professionSounds;


    public ProfessionVoiceConfig(SoundEntry sound, String voicepackId) {
        super(sound, voicepackId);
        professionSounds = new ArrayList<>();
    }

    @Override
    public String toJsonString() {
        String out = "{\n" + "\"id\": \"" + id + "\",\n";
        out += "\"voicepack_id\": \"" + voicepack_id + "\",\n";
        out += "\"type\": \"" + type.toString() + "\",\n";
        for (int i = 0; i < professionSounds.size(); i++) {
            VoiceProfession voiceProfession = professionSounds.get(i);
            out += voiceProfession.toJsonString() + "\n";
            out += (i != professionSounds.size() - 1) ? ",\n" : "\n";
        }
        out += "}";
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(out);
            TalkingVillagers.LOGGER.debug(gson.toJson(je));
            return gson.toJson(je);
        } catch (Exception e){
            TalkingVillagers.LOGGER.error(e);
        }
        return "";
    }

    @Override
    public void addSound(SoundEntry sound) {
        String[] profs;
        profs = sound.getProfessions().replace(" ", "").split(",");
        for (var prof : profs) {
            addProfSound(prof, sound);
        }
    }

    private void addProfSound(String profession, SoundEntry sound) {
        VoiceProfession voiceProfession = null;
        if(professionSounds == null) {
            professionSounds = new ArrayList<>();
        }
        for (var profSound : professionSounds) {
            if(profSound.getName().equals(profession)) {
                voiceProfession = profSound;
                break;
            }
        }
        if(voiceProfession == null) {
            voiceProfession = new VoiceProfession(profession);
            professionSounds.add(voiceProfession);
        }
        voiceProfession.addSound(sound);
    }

    public ArrayList<VoiceProfession> getProfessionSounds() {
        return professionSounds;
    }
}

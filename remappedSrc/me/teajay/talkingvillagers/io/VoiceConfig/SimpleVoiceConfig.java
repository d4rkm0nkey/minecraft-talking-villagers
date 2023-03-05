package me.teajay.talkingvillagers.io.VoiceConfig;

import ;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.teajay.talkingvillagers.helpers.StringHelpers;
import me.teajay.talkingvillagers.io.SoundEntry;
import me.teajay.talkingvillagers.io.VoiceEvent;

import java.util.ArrayList;

public class SimpleVoiceConfig extends VoiceConfig {
    private ArrayList<VoiceEvent> eventSounds = new ArrayList<>();

    public SimpleVoiceConfig(SoundEntry sound, String voicepackId) {
        super(sound, voicepackId);
    }

    @Override
    public String toJsonString() {
        String out = "{\n" + "\"id\": " + id + ",\n";
        out += "\"voicepack_id\": " + voicepack_id + ",\n";
        out += "\"type\": " + type.toString() + ",\n";
        for (int i = 0; i < eventSounds.size(); i++) {
            VoiceEvent voiceProfession = eventSounds.get(i);
            out += voiceProfession.toJsonString() + ",/n";
            out += (i != eventSounds.size() - 1) ? ",/n" : "/n";
        }
        out += "}";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(out);
        return gson.toJson(je);
    }

    @Override
    public void addSound(SoundEntry sound) {
        VoiceEvent event = null;
        for (var eventSound: eventSounds) {
            if(eventSound.getName().equals(sound.getEvent())) {
                event = eventSound;
                break;
            }
        }
        if(event == null) {
            event = new VoiceEvent(sound.getEvent());
            eventSounds.add(event);
        }
        var soundPath = sound.getSounds().get(0).split("/");
        String soundName = StringHelpers.removeNumbers(soundPath[soundPath.length-1]);
        event.addSound(soundName);
    }
}

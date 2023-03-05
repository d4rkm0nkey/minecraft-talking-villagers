package me.teajay.talkingvillagers.io;

import me.teajay.talkingvillagers.helpers.StringHelpers;
import ;
import java.util.ArrayList;

public class VoiceProfession {
    private String name;
    private ArrayList<VoiceEvent> eventSounds;
    public String toJsonString() {
        String out = "\"" + name + "\": {\n";
        for (int i = 0; i < eventSounds.size(); i++) {
            VoiceEvent eventSound = eventSounds.get(i);
            out += eventSound.toJsonString();
            out += (i != eventSounds.size() - 1) ? ",/n" : "/n";
        }
        out += "}\n";
        return out;
    }

    public String getName() {
        return name;
    }

    public VoiceProfession(String name) {
        this.name = name;
        eventSounds = new ArrayList<>();
    }

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

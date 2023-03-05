package me.teajay.talkingvillagers.io;

import java.util.ArrayList;

public class VoiceEvent {
    private String name;
    private ArrayList<String> sounds = new ArrayList<>();

    public String toJsonString() {
        String out = "\"" + name + "\": [\n";
        for (int i = 0; i < sounds.size(); i++) {
            String sound = sounds.get(i);
            out += "\"" + sound + "\"";
            out += (i != sounds.size() - 1) ? ",\n" : "\n";
        }
        out += "]";
        return out;
    }

    public VoiceEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addSound(String soundName) {
        this.sounds.add(soundName);
    }
}

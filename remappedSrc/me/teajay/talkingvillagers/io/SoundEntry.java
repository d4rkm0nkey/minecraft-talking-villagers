package me.teajay.talkingvillagers.io;

import java.util.ArrayList;

public class SoundEntry {
    private String voiceId;
    private String type;
    private String event;
    private String professions;
    private ArrayList<String> sounds;

    public String getVoiceId() {
        return voiceId;
    }

    public String getType() {
        return type;
    }

    public String getEvent() {
        return event;
    }

    public String getProfessions() {
        return professions;
    }

    public ArrayList<String> getSounds() {
        return sounds;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }
}

package me.teajay.talkingvillagers.io;

public class VoiceInfo {
    public enum VoiceType {
        VILLAGER,
        WANDERING_TRADER,
        PILLAGER,
        WITCH
    }
    private String id;
    private String name;
    private VoiceType type;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VoiceType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "VoiceInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}

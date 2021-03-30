package me.teajay.talking.villagers.common.data;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;

import java.util.ArrayList;

public class VoiceData {
    private final ArrayList<String> gossip;
    private final ArrayList<String> death;
    private final ArrayList<String> random;
    private final ArrayList<String> trade;
    private final ArrayList<String> hurt;
    private final ArrayList<String> eat;
    private final ArrayList<String> hero;
    private final ArrayList<String> nope;
    private final ArrayList<String> level;


    public VoiceData() {
        gossip = new ArrayList<>();
        death = new ArrayList<>();
        random = new ArrayList<>();
        trade = new ArrayList<>();
        hurt = new ArrayList<>();
        nope = new ArrayList<>();
        eat = new ArrayList<>();
        hero = new ArrayList<>();
        level = new ArrayList<>();
    }

    public ArrayList<String> getVoiceLines(VillagerVoiceManager.Reason reason) {
        switch (reason) {
            case RANDOM:
                return random;
            case GOSSIP:
                return gossip;
            case NO:
                return nope;
            case EAT:
                return eat;
            case HERO:
                return hero;
            case HURT:
                return hurt;
            case DEATH:
                return death;
            case LEVEL:
                return level;
            case TRADE:
                return trade;
        }
        return null;
    }
}

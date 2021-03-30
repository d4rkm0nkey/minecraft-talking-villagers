package me.teajay.talking.villagers.common.data;

import com.google.common.collect.Lists;

import java.util.ArrayList;

public class VoiceData {
    private ArrayList<String> gossip;
    private ArrayList<String> death;
    private ArrayList<String> random;
    private ArrayList<String> trade;
    public VoiceData() {
        gossip = new ArrayList();
        death = new ArrayList();
        random = new ArrayList();
        trade = new ArrayList();
    }

    public ArrayList<String> getGossip() {
        return gossip;
    }

    public ArrayList<String> getDeath() {
        return death;
    }

    public ArrayList<String> getRandom() {
        return random;
    }

    public ArrayList<String> getTrade() {
        return trade;
    }
}

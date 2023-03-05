package me.teajay.talking.villagers.common.data;

import java.util.ArrayList;

public class VoiceData {
    protected final ArrayList<String> gossip;
    protected final ArrayList<String> death;
    protected final ArrayList<String> random;
    protected final ArrayList<String> trade;
    protected final ArrayList<String> tradesuccess;
    protected final ArrayList<String> hurt;
    protected final ArrayList<String> hero;
    protected final ArrayList<String> nope;
    protected final ArrayList<String> level;
    protected final ArrayList<String> ambient;
    protected final ArrayList<String> greeting;
    protected final ArrayList<String> herodrop;
    protected final ArrayList<String> yes;
    protected final ArrayList<String> celebrate;
    protected final ArrayList<String> good_weather;
    protected final ArrayList<String> bad_weather;

    public VoiceData() {
        gossip = new ArrayList<>();
        death = new ArrayList<>();
        random = new ArrayList<>();
        trade = new ArrayList<>();
        tradesuccess = new ArrayList<>();
        hurt = new ArrayList<>();
        nope = new ArrayList<>();
        hero = new ArrayList<>();
        level = new ArrayList<>();
        ambient = new ArrayList<>();
        greeting = new ArrayList<>();
        herodrop = new ArrayList<>();
        yes = new ArrayList<>();
        celebrate = new ArrayList<>();
        good_weather = new ArrayList<>();
        bad_weather = new ArrayList<>();
    }
}

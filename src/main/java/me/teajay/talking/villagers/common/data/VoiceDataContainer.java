package me.teajay.talking.villagers.common.data;

import me.teajay.talking.villagers.common.sound.VillagerVoiceManager;
import net.minecraft.village.VillagerProfession;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;

public class VoiceDataContainer {
    private String name;

    private final VoiceData all;
    private final VoiceData nitwit;
    private final VoiceData farmer;
    private final VoiceData librarian;
    private final VoiceData cartographer;
    private final VoiceData toolsmith;
    private final VoiceData fletcher;
    private final VoiceData sheperd;
    private final VoiceData leatherworker;
    private final VoiceData mason;
    private final VoiceData fischerman;
    private final VoiceData cleric;
    private final VoiceData armorer;
    private final VoiceData butcher;
    private final VoiceData weaponsmith;

    public enum Profession {
        ARMORER(VillagerProfession.ARMORER),
        BUTCHER(VillagerProfession.BUTCHER),
        CARTOGRAPHER(VillagerProfession.CARTOGRAPHER),
        CLERIC(VillagerProfession.CLERIC),
        FARMER(VillagerProfession.FARMER),
        FISHERMAN(VillagerProfession.FISHERMAN),
        FLETCHER(VillagerProfession.FLETCHER),
        LEATHERWORKER(VillagerProfession.LEATHERWORKER),
        LIBRARIAN(VillagerProfession.LIBRARIAN),
        MASON(VillagerProfession.MASON),
        NITWIT(VillagerProfession.NITWIT),
        SHEPHERD(VillagerProfession.SHEPHERD),
        TOOLSMITH(VillagerProfession.TOOLSMITH),
        WEAPONSMITH(VillagerProfession.WEAPONSMITH);
        public VillagerProfession getProfession() {
            return profession;
        }
        private VillagerProfession profession;
        private Profession(VillagerProfession prof) {
            this.profession = prof;
        }
    }



    public VoiceDataContainer() {
        all = new VoiceData();
        nitwit = new VoiceData();
        farmer = new VoiceData();
        librarian = new VoiceData();
        cartographer = new VoiceData();
        toolsmith = new VoiceData();
        fletcher = new VoiceData();
        sheperd = new VoiceData();
        leatherworker = new VoiceData();
        mason = new VoiceData();
        fischerman = new VoiceData();
        cleric = new VoiceData();
        armorer = new VoiceData();
        butcher = new VoiceData();
        weaponsmith = new VoiceData();
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getDefaultVoiceLines(VillagerVoiceManager.Reason reason) {
        switch (reason) {
            case RANDOM:
                return all.random;
            case GOSSIP:
                return all.gossip;
            case NO:
                return all.nope;
            case EAT:
                return all.eat;
            case HERO:
                return all.hero;
            case HURT:
                return all.hurt;
            case DEATH:
                return all.death;
            case LEVEL:
                return all.level;
            case TRADE:
                return all.trade;
            case TRADE_SUCCESS:
                return all.tradesuccess;
            case AMBIENT:
                return all.ambient;
            case GREETING:
                return all.greeting;
            case HERODROP:
                return all.herodrop;
            case YES:
                return all.yes;
            case CELEBRATE:
                return all.celebrate;
        }
        return new ArrayList<>();
    }

    public ArrayList<String> getProfessionVoiceLines(VillagerVoiceManager.Reason reason, Profession profession) {
        VoiceData professionData;
        switch (profession) {
            case ARMORER:
                professionData = armorer;
                break;
            case FARMER:
                professionData = farmer;
                break;
            case LIBRARIAN:
                professionData = librarian;
                break;
            case CARTOGRAPHER:
                professionData = cartographer;
                break;
            case TOOLSMITH:
                professionData = toolsmith;
                break;
            case FLETCHER:
                professionData = fletcher;
                break;
            case SHEPHERD:
                professionData = sheperd;
                break;
            case LEATHERWORKER:
                professionData = leatherworker;
                break;
            case MASON:
                professionData = mason;
                break;
            case FISHERMAN:
                professionData = fischerman;
                break;
            case CLERIC:
                professionData = cleric;
                break;
            case BUTCHER:
                professionData = butcher;
                break;
            case WEAPONSMITH:
                professionData = weaponsmith;
                break;
            case NITWIT:
                professionData = nitwit;
                break;
            default:
                return new ArrayList<>();
        }
        switch (reason) {
            case RANDOM:
                return professionData.random;
            case GOSSIP:
                return professionData.gossip;
            case NO:
                return professionData.nope;
            case EAT:
                return professionData.eat;
            case HERO:
                return professionData.hero;
            case HURT:
                return professionData.hurt;
            case DEATH:
                return professionData.death;
            case LEVEL:
                return professionData.level;
            case TRADE:
                return professionData.trade;
            case TRADE_SUCCESS:
                return professionData.tradesuccess;
            case AMBIENT:
                return professionData.ambient;
            case GREETING:
                return professionData.greeting;
            case HERODROP:
                return professionData.herodrop;
            case YES:
                return professionData.yes;
            case CELEBRATE:
                return professionData.celebrate;
        }
        return new ArrayList<>();
    }
}

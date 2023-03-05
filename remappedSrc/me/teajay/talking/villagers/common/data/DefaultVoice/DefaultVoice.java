package me.teajay.talking.villagers.common.data.DefaultVoice;

public class DefaultVoice {
    private String name = "teajay";
    private ProfessionVoiceLines farmer;
    private ProfessionVoiceLines cleric;
    private ProfessionVoiceLines armorer;
    private ProfessionVoiceLines all;
    private ProfessionVoiceLines toolsmith;
    private ProfessionVoiceLines cartograph;
    private ProfessionVoiceLines weaponsmith;
    private ProfessionVoiceLines butcher;
    private ProfessionVoiceLines librarian;
    private ProfessionVoiceLines fisherman;
    private ProfessionVoiceLines leatherworker;
    private ProfessionVoiceLines mason;
    private ProfessionVoiceLines fletcher;
    private ProfessionVoiceLines nitwit;
    private ProfessionVoiceLines shepherd;

    public DefaultVoice(String name, ProfessionVoiceLines farmer, ProfessionVoiceLines cleric, ProfessionVoiceLines armorer, ProfessionVoiceLines all, ProfessionVoiceLines toolsmith, ProfessionVoiceLines cartograph, ProfessionVoiceLines weaponsmith, ProfessionVoiceLines butcher, ProfessionVoiceLines librarian, ProfessionVoiceLines fisherman, ProfessionVoiceLines leatherworker, ProfessionVoiceLines mason, ProfessionVoiceLines fletcher, ProfessionVoiceLines nitwit, ProfessionVoiceLines shepherd) {
        this.name = name;
        this.farmer = farmer;
        this.cleric = cleric;
        this.armorer = armorer;
        this.all = all;
        this.toolsmith = toolsmith;
        this.cartograph = cartograph;
        this.weaponsmith = weaponsmith;
        this.butcher = butcher;
        this.librarian = librarian;
        this.fisherman = fisherman;
        this.leatherworker = leatherworker;
        this.mason = mason;
        this.fletcher = fletcher;
        this.nitwit = nitwit;
        this.shepherd = shepherd;
    }

    public static DefaultVoice createDefaultVoice() {
        return new DefaultVoice();
    }

    private DefaultVoice() {
        farmer = ProfessionVoiceLines.farmer();
        cleric = ProfessionVoiceLines.cleric();
        armorer = ProfessionVoiceLines.armorer();
        all = ProfessionVoiceLines.all();
        toolsmith = ProfessionVoiceLines.toolsmith();
        cartograph = ProfessionVoiceLines.cartograph();
        weaponsmith = ProfessionVoiceLines.weaponsmith();
        butcher = ProfessionVoiceLines.butcher();
        librarian = ProfessionVoiceLines.librarian();
        fisherman = ProfessionVoiceLines.fisherman();
        leatherworker = ProfessionVoiceLines.leatherworker();
        mason = ProfessionVoiceLines.mason();
        fletcher = ProfessionVoiceLines.fletcher();
        nitwit = ProfessionVoiceLines.nitwit();
        shepherd = ProfessionVoiceLines.shepherd();
    }
}

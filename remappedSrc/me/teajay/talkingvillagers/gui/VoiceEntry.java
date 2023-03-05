package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import me.teajay.talkingvillagers.voicepacks.VoiceFile;
import me.teajay.talkingvillagers.voicepacks.Voicepack;
import net.minecraft.text.LiteralText;

import java.util.function.Function;

public class VoiceEntry extends SpruceContainerWidget {
    public static final String VILLAGER_SYMBOL = "☻";
    public static final String PILLAGER_SYMBOL = "\uD83C\uDFF9";
    public static final String WITCH_SYMBOL = "☠";
    public static final String WANDERING_TRADER_SYMBOL = "☀";

    public VoiceEntry(Voicepack voicepack, VoiceFile voice, Position position, int width, int height, Lambda callBack) {
        super(position, width, height);
        this.addChild(new SpruceLabelWidget(Position.of(position, 40, 2), new LiteralText("▶ " + voice.getName()), width/3));
        var symbol = "?";
        switch (voice.getType()) {
            case VILLAGER -> symbol = VILLAGER_SYMBOL;
            case PILLAGER -> symbol = PILLAGER_SYMBOL;
            case WITCH -> symbol = WITCH_SYMBOL;
            case WANDERING_TRADER -> symbol = WANDERING_TRADER_SYMBOL;
        }
        SpruceButtonWidget.PressAction action = new VoiceInstallAction(voicepack, voice, callBack);
        LiteralText text = (voice.isInstalled()) ? new LiteralText("Uninstall") : new LiteralText("Install");
        this.addChild(new SpruceLabelWidget(Position.of(position, width/3+40, 2), new LiteralText(symbol), width/3-40));
        this.addChild(new SpruceButtonWidget(
                Position.of(position, width - width / 3, 0),
                width/3-20,
                height-8,
                text,
                action)
        );
    }
}

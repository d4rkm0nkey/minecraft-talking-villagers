package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceLabelWidget;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.voicepacks.Voicepack;
import me.teajay.talkingvillagers.voicepacks.VoicepackManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;

public class VoicesScreen extends SpruceScreen {
    private final Screen parent;

    public VoicesScreen(@Nullable Screen parent) {
        super(new TranslatableText("talkingvillagers.voicescreen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        refresh();
    }

    private void refresh() {
        this.clearChildren();
        var loadingText = drawLoadingText();
        var voicepacks = VoicepackManager.getVoicePacks();
        this.remove(loadingText);
        if(voicepacks.isEmpty()) {
            drawNoVoicePacksInstalledMessage();
        } else {
            drawVoicePackList(voicepacks);
        }
        drawSettingsButton();
        drawOpenVoicepacksFolderButton();
        drawDoneButton();
    }

    private void drawVoicePackList(ArrayList<Voicepack> voicePacks) {
        var list = new VoicePackEntryList(Position.of(this.width/2-200, 25), 400, this.height-60,0, VoicePackEntry.class);
        list.add(voicePacks, ()->refresh());
        this.addDrawableChild(list);
    }

    private void drawNoVoicePacksInstalledMessage() {
        this.addDrawableChild(new SpruceLabelWidget(Position.of(this.width/2-100, 40), new TranslatableText("talkingvillagers.voicescreen.text.no_voices_installed"), 200));
    }

    private SpruceLabelWidget drawLoadingText() {
        var loadingText = new SpruceLabelWidget(Position.of(this.width/2-100, 40), new TranslatableText("talkingvillagers.voicescreen.text.no_voices_installed"), 200);
        this.addDrawableChild(loadingText);
        return loadingText;
    }

    private void drawOpenVoicepacksFolderButton() {
        this.addDrawableChild(new SpruceButtonWidget(Position.of(this, this.width / 2 - 75, this.height - 29), 150, 20, new TranslatableText("talkingvillager.voicescreen.button.voicepackfolder"),
                btn -> openVoicePackFolder()));
    }

    private void openVoicePackFolder() {
        try {
            System.setProperty("java.awt.headless", "false");
            Desktop.getDesktop().open(Path.of(FabricLoader.getInstance().getGameDir().toString(), "resourcepacks").toFile());
        } catch (Exception e) {
            TalkingVillagers.LOGGER.error(e);
        }
    }

    private void drawDoneButton() {
        this.addDrawableChild(new SpruceButtonWidget(Position.of(this, this.width / 2 + 76, this.height - 29), 125, 20, new TranslatableText("talkingvillager.voicescreen.button.done"),
                btn -> this.client.setScreen(this.parent)));
    }

    private void drawSettingsButton() {
        var settingsScreen = new ModMenuSettingsScreen().getModConfigScreenFactory().create(this);
        this.addDrawableChild(new SpruceButtonWidget(Position.of(this, this.width / 2 - 200, this.height - 29), 125, 20, new TranslatableText("talkingvillager.voicescreen.button.settings"),
                btn -> this.client.setScreen(settingsScreen)));
    }

    @Override
    public void renderTitle(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 16777215);
    }
}

package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import me.teajay.talkingvillagers.voicepacks.Voicepack;
import me.teajay.talkingvillagers.voicepacks.VoicepackManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.LiteralText;

public class VoicePackInstallAction implements SpruceButtonWidget.PressAction {
    private SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
    private final Lambda callback;
    private Voicepack voicepack;

    public VoicePackInstallAction(Voicepack voicepack, Lambda callBack) {
        this.voicepack = voicepack;
        this.callback = callBack;
    }

    @Override
    public void onPress(SpruceButtonWidget button) {
        if(voicepack.isFullyInstalled()) {
            button.setMessage(new LiteralText("Uninstalling..."));
            if(VoicepackManager.uninstallVoicepack(voicepack)) {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.VOICEPACK_UNINSTALLED_EVENT, 1.f));
            } else {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.UNINSTALL_ERROR_EVENT, 1.f));
            }
            callback.run();
        } else {
            button.setMessage(new LiteralText("Installing..."));
            if(VoicepackManager.installVoicepack(voicepack)) {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.VOICEPACK_INSTALLED_EVENT, 1.f));
            } else {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.INVALID_VOICEPACK_EVENT, 1.f));
            }
            callback.run();
        }
    }
}

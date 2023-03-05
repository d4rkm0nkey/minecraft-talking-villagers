package me.teajay.talkingvillagers.gui;

import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.voicepacks.VoiceFile;
import me.teajay.talkingvillagers.voicepacks.Voicepack;
import me.teajay.talkingvillagers.voicepacks.VoicepackManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.LiteralText;

public class VoiceInstallAction implements SpruceButtonWidget.PressAction {
    private SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
    private final Lambda callback;
    VoiceFile voice;
    Voicepack voicepack;


    public VoiceInstallAction(Voicepack voicepack, VoiceFile voice, Lambda callBack) {
        this.voice = voice;
        this.voicepack = voicepack;
        this.callback = callBack;
    }
    @Override
    public void onPress(SpruceButtonWidget button) {
        TalkingVillagers.LOGGER.debug("Pressed!");
        if(voice.isInstalled()) {
            button.setMessage(new LiteralText("Uninstalling..."));
            if(VoicepackManager.uninstallVoice(voice)) {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.VOICE_UNINSTALLED_EVENT, 1.f));
            } else {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.UNINSTALL_ERROR_EVENT, 1.f));
            }
            callback.run();
        } else {
            button.setMessage(new LiteralText("Installing..."));
            if(VoicepackManager.installVoice(voicepack.getSounds(), voice)) {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.VOICE_INSTALLED_EVENT, 1.f));
            } else {
                this.soundManager.play(PositionedSoundInstance.master(MenuSounds.INVALID_VOICEPACK_EVENT, 1.f));
            }
            callback.run();
        }
    }
}

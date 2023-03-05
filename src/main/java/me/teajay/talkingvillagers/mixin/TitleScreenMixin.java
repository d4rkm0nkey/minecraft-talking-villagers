package me.teajay.talkingvillagers.mixin;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceTexturedButtonWidget;
import me.teajay.talkingvillagers.gui.VoicesScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final Identifier VOICE_ICON_TEXTURE = new Identifier("textures/gui/voice.png");

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int j = this.height / 4 + 48;

        this.addDrawableChild(
            new SpruceTexturedButtonWidget(
                Position.of(this.width / 2 + 104, j + 45),
                32,
                32,
                Text.translatable("menu.voiceinstall"),
                false,
                btn -> this.client.setScreen(new VoicesScreen(this)),
                0,
                0,
                3,
                VOICE_ICON_TEXTURE,
                32,
                32
            ).asVanilla()
        );
    }
}

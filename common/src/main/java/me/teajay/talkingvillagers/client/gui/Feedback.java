package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

import java.net.URI;
import java.util.List;

/**
 * The link to the player survey, shown in beta versions only. It just opens the browser; the mod sends nothing.
 */
final class Feedback {
    static final URI SURVEY = URI.create("https://tally.so/r/zExyRM");

    private Feedback() {
    }

    /** Adds the survey row while the mod is a beta, so it disappears with the release by itself. */
    static void addRow(List<RowList.Row> rows, Screen screen, Font font) {
        if (!Services.PLATFORM.getModVersion().contains("-beta")) {
            return;
        }
        rows.add(new RowList.RowBuilder(font)
                .line(GuiTexts.key("feedback").copy().withStyle(ChatFormatting.AQUA))
                .secondary(GuiTexts.key("feedback.hint"))
                .widget(Button.builder(GuiTexts.key("feedback.button"), ConfirmLinkScreen.confirmLink(screen, SURVEY))
                        .width(80).tooltip(Tooltip.create(GuiTexts.key("feedback.tooltip"))).build())
                .build());
    }
}

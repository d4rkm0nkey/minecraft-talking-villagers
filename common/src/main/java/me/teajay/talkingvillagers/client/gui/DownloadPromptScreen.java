package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PackDownloads;
import me.teajay.talkingvillagers.network.ServerPacksPayload.PackInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Asks whether to download the voicepacks the server uses.
 */
public class DownloadPromptScreen extends Screen {
    private final List<PackInfo> packs;
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public DownloadPromptScreen(List<PackInfo> packs) {
        super(GuiTexts.key("download.title"));
        this.packs = List.copyOf(packs);
    }

    @Override
    protected void init() {
        layout = new HeaderAndFooterLayout(this, 33, 60);
        layout.addTitleHeader(title, font);
        LinearLayout content = layout.addToContents(LinearLayout.vertical().spacing(4));
        content.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(new MultiLineTextWidget(GuiTexts.key("download.message"), font).setMaxWidth(Math.min(width - 40, 360)).setCentered(true));
        long total = 0;
        for (PackInfo pack : packs.subList(0, Math.min(packs.size(), 6))) {
            content.addChild(new StringWidget(Component.literal(pack.name() + " " + pack.version() + " (").append(GuiTexts.size(pack.size())).append(")").withStyle(ChatFormatting.GRAY), font));
        }
        for (PackInfo pack : packs) {
            total += pack.size();
        }
        if (packs.size() > 6) {
            content.addChild(new StringWidget(GuiTexts.key("download.more", packs.size() - 6).copy().withStyle(ChatFormatting.GRAY), font));
        }

        LinearLayout footer = layout.addToFooter(LinearLayout.vertical().spacing(4));
        footer.defaultCellSetting().alignHorizontallyCenter();
        LinearLayout row = footer.addChild(LinearLayout.horizontal().spacing(8));
        row.addChild(Button.builder(GuiTexts.key("download.yes", GuiTexts.size(total)), button -> answer(true, false)).width(150).build());
        row.addChild(Button.builder(GuiTexts.key("download.no"), button -> onClose()).width(150).build());
        if (!PackDownloads.serverKey().isEmpty()) {
            footer.addChild(Button.builder(GuiTexts.key("download.always"), button -> answer(true, true)).width(308).build());
        }
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private void answer(boolean download, boolean always) {
        if (download) {
            PackDownloads.accept(packs, always);
        }
        onClose();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }
}

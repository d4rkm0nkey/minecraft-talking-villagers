package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A screen with a title, a {@link RowList} and a back button.
 */
public abstract class ListScreen extends Screen {
    protected final Screen parent;
    protected HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    protected @Nullable RowList list;

    protected ListScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    protected abstract List<RowList.Row> rows();

    @Override
    protected void init() {
        layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);
        list = layout.addToContents(new RowList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight()));
        layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).width(200).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        refresh();
    }

    protected void refresh() {
        if (list != null) {
            double scroll = list.scrollAmount();
            list.setRows(rows());
            list.setScrollAmount(scroll);
        }
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSizeAndPosition(width, layout.getContentHeight(), layout.getHeaderHeight());
        }
    }

    @Override
    public void onClose() {
        PreviewSounds.stop();
        minecraft.gui.setScreen(parent);
    }
}

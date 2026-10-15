package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A screen with a title, a {@link RowList} and a back button.
 */
public abstract class ListScreen extends Screen {
    protected final Screen parent;
    protected HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    protected @Nullable RowList list;
    private @Nullable EditBox search;
    private String query = "";

    protected ListScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    protected abstract List<RowList.Row> rows();

    /** Screens with a search field above the list override this. */
    protected boolean searchable() {
        return false;
    }

    /** The search text in lower case, empty without search. */
    protected String query() {
        return query;
    }

    /** Whether any of the texts contains the search text. */
    protected boolean matches(String... texts) {
        if (query.isEmpty()) {
            return true;
        }
        for (String text : texts) {
            if (text != null && text.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
        if (searchable()) {
            layout = new HeaderAndFooterLayout(this, 48, 33);
            LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
            header.defaultCellSetting().alignHorizontallyCenter();
            header.addChild(new StringWidget(title, font));
            search = header.addChild(new EditBox(font, 0, 0, 200, 15, GuiTexts.key("packs.search")));
            search.setHint(GuiTexts.key("packs.search").copy().withStyle(EditBox.SEARCH_HINT_STYLE));
            search.setValue(query);
            search.setResponder(value -> {
                query = value.toLowerCase(Locale.ROOT).trim();
                list.setRows(rows());
            });
        } else {
            layout = new HeaderAndFooterLayout(this);
            layout.addTitleHeader(title, font);
        }
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
    protected void setInitialFocus() {
        if (search != null) {
            setInitialFocus(search);
        } else {
            super.setInitialFocus();
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

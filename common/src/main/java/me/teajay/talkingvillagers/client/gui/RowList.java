package me.teajay.talkingvillagers.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrolling list of rows with text on the left and buttons on the right, used by all screens of the mod.
 */
public class RowList extends ContainerObjectSelectionList<RowList.Row> {
    public static final int TEXT_ROW_HEIGHT = 12;
    public static final int WIDGET_ROW_HEIGHT = 24;
    public static final int ICON_ROW_HEIGHT = 36;
    private static final int MAX_ROW_WIDTH = 420;
    private static final int GRAY = 0xFFA0A0A0;

    public RowList(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, WIDGET_ROW_HEIGHT);
        this.centerListVertically = false;
    }

    public void setRows(List<Row> rows) {
        clearEntries();
        rows.forEach(row -> addEntry(row, row.height()));
        setScrollAmount(0);
    }

    @Override
    public int getRowWidth() {
        return Math.min(MAX_ROW_WIDTH, width - 24);
    }

    /**
     * A row: up to three lines of text, an optional icon and buttons aligned to the right.
     */
    public static class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Font font;
        private final @Nullable Identifier icon;
        private final List<Component> lines;
        private final List<Integer> colors;
        private final List<AbstractWidget> widgets;
        private final boolean centered;
        private final int height;

        private Row(Font font, @Nullable Identifier icon, List<Component> lines, List<Integer> colors, List<AbstractWidget> widgets,
                    boolean centered, int height) {
            this.font = font;
            this.icon = icon;
            this.lines = lines;
            this.colors = colors;
            this.widgets = widgets;
            this.centered = centered;
            this.height = height;
        }

        public int height() {
            return height;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int right = getContentRight();
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget widget = widgets.get(i);
                right -= widget.getWidth();
                widget.setPosition(right, getContentYMiddle() - widget.getHeight() / 2);
                widget.extractRenderState(graphics, mouseX, mouseY, a);
                right -= 4;
            }

            int x = getContentX();
            if (icon != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
                x += 36;
            }
            int textWidth = right - x - 4;
            int lineHeight = font.lineHeight + 2;
            int y = getContentYMiddle() - (lines.size() * lineHeight - 2) / 2;
            for (int i = 0; i < lines.size(); i++) {
                Component line = lines.get(i);
                if (centered) {
                    graphics.centeredText(font, line, getContentXMiddle(), y, colors.get(i));
                } else {
                    String text = line.getString();
                    String fitted = font.width(text) > textWidth ? font.plainSubstrByWidth(text, textWidth - font.width("…")) + "…" : null;
                    if (fitted == null) {
                        graphics.text(font, line, x, y, colors.get(i));
                    } else {
                        graphics.text(font, fitted, x, y, colors.get(i));
                        if (mouseX >= x && mouseX < x + textWidth && mouseY >= y && mouseY < y + font.lineHeight) {
                            graphics.setTooltipForNextFrame(line, mouseX, mouseY);
                        }
                    }
                }
                y += lineHeight;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }

    /**
     * Builds rows; the height follows from the content.
     */
    public static final class RowBuilder {
        private final Font font;
        private @Nullable Identifier icon;
        private final List<Component> lines = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private boolean centered;

        public RowBuilder(Font font) {
            this.font = font;
        }

        public RowBuilder icon(Identifier icon) {
            this.icon = icon;
            return this;
        }

        public RowBuilder line(Component text) {
            return line(text, 0xFFFFFFFF);
        }

        public RowBuilder secondary(Component text) {
            return line(text, GRAY);
        }

        public RowBuilder line(Component text, int color) {
            lines.add(text);
            colors.add(color);
            return this;
        }

        public RowBuilder widget(AbstractWidget widget) {
            widgets.add(widget);
            return this;
        }

        public RowBuilder centered() {
            this.centered = true;
            return this;
        }

        public Row build() {
            int textHeight = lines.size() * (font.lineHeight + 2) + 2;
            int height = Math.max(textHeight, icon != null ? ICON_ROW_HEIGHT : widgets.isEmpty() ? TEXT_ROW_HEIGHT : WIDGET_ROW_HEIGHT);
            return new Row(font, icon, List.copyOf(lines), List.copyOf(colors), List.copyOf(widgets), centered, height);
        }
    }
}

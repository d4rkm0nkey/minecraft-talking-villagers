package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The events a voice has lines for, each with a sample button; clicking an event shows its lines. The search also finds
 * events by the text of their lines.
 */
public class VoiceEventsScreen extends ListScreen {
    private final LoadedPack pack;
    private final ResolvedVoice voice;
    private final Random random = new Random();

    public VoiceEventsScreen(Screen parent, LoadedPack pack, ResolvedVoice voice) {
        super(parent, GuiTexts.key("events.title", voice.voice().name()));
        this.pack = pack;
        this.voice = voice;
    }

    @Override
    protected boolean searchable() {
        return true;
    }

    @Override
    protected List<RowList.Row> rows() {
        List<RowList.Row> rows = new ArrayList<>();
        for (VoiceEvent event : EventCatalog.all()) {
            List<VoiceLine> lines = voice.voice().lines(event);
            if (lines.isEmpty() || !matches(event, lines)) {
                continue;
            }
            Button play = Button.builder(Component.literal("▶"), button -> PreviewSounds.play(pack, lines.get(random.nextInt(lines.size())), 1.0f))
                    .width(20).tooltip(Tooltip.create(GuiTexts.key("details.play"))).build();
            rows.add(new RowList.RowBuilder(font)
                    .line(Component.literal(event.id()).withStyle(ChatFormatting.BOLD)
                            .append(Component.literal("  ").append(GuiTexts.key("details.lines", lines.size())).withStyle(ChatFormatting.GRAY)))
                    .secondary(Component.translatable("talkingvillagers.event." + event.id()))
                    .widget(play)
                    .widget(Button.builder(GuiTexts.key("details.show_lines").copy().append(" ›"),
                            button -> minecraft.gui.setScreen(new EventLinesScreen(this, pack, voice, event, query()))).width(64).build())
                    .build());
        }
        if (rows.isEmpty()) {
            rows.add(new RowList.RowBuilder(font).secondary(GuiTexts.key("events.none")).centered().build());
        }
        return rows;
    }

    private boolean matches(VoiceEvent event, List<VoiceLine> lines) {
        if (matches(event.id(), Component.translatable("talkingvillagers.event." + event.id()).getString())) {
            return true;
        }
        return lines.stream().anyMatch(line -> matches(line.text().orElse(""), line.file()));
    }
}

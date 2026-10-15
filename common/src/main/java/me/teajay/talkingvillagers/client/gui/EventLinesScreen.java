package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines of one event of a voice, each playable, with search by text and file name.
 */
public class EventLinesScreen extends ListScreen {
    private final LoadedPack pack;
    private final ResolvedVoice voice;
    private final VoiceEvent event;
    private final String initialQuery;

    public EventLinesScreen(Screen parent, LoadedPack pack, ResolvedVoice voice, VoiceEvent event, String initialQuery) {
        super(parent, Component.literal(voice.voice().name() + " › " + event.id()));
        this.pack = pack;
        this.voice = voice;
        this.event = event;
        this.initialQuery = initialQuery;
    }

    @Override
    protected boolean searchable() {
        return voice.voice().lines(event).size() > 5;
    }

    @Override
    protected List<RowList.Row> rows() {
        List<RowList.Row> rows = new ArrayList<>();
        rows.add(new RowList.RowBuilder(font).secondary(Component.translatable("talkingvillagers.event." + event.id())).build());
        List<VoiceLine> lines = voice.voice().lines(event);
        // the search of the event list carries over, so a line found there is shown directly
        String filter = searchable() ? query() : initialQuery;
        for (int i = 0; i < lines.size(); i++) {
            VoiceLine line = lines.get(i);
            String file = line.file().substring(line.file().lastIndexOf('/') + 1);
            boolean eventMatches = event.id().contains(filter);
            if (!filter.isEmpty() && !eventMatches && !line.text().orElse("").toLowerCase(Locale.ROOT).contains(filter) && !file.contains(filter)) {
                continue;
            }
            String details = file + line.duration().map(d -> String.format(Locale.ROOT, " · %.1f s", d)).orElse("") + conditions(line.conditions());
            rows.add(new RowList.RowBuilder(font)
                    .line(Component.literal("#" + i + " " + line.text().orElse("")))
                    .secondary(Component.literal(details))
                    .widget(Button.builder(Component.literal("▶"), button -> PreviewSounds.play(pack, line, 1.0f)).width(20).build())
                    .build());
        }
        if (rows.size() == 1) {
            rows.add(new RowList.RowBuilder(font).secondary(GuiTexts.key("events.none")).centered().build());
        }
        return rows;
    }

    private static String conditions(LineConditions conditions) {
        StringBuilder text = new StringBuilder();
        if (!conditions.professions().isEmpty()) {
            text.append(" · ").append(String.join(", ", conditions.professions()));
        }
        conditions.weather().ifPresent(weather -> text.append(" · ").append(weather.id()));
        return text.toString();
    }
}

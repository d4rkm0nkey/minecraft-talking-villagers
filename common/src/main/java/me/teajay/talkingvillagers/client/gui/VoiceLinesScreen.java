package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every line of a voice, grouped by event, each playable.
 */
public class VoiceLinesScreen extends ListScreen {
    private final LoadedPack pack;
    private final ResolvedVoice voice;

    public VoiceLinesScreen(Screen parent, LoadedPack pack, ResolvedVoice voice) {
        super(parent, Component.literal(voice.voice().name()));
        this.pack = pack;
        this.voice = voice;
    }

    @Override
    protected List<RowList.Row> rows() {
        List<RowList.Row> rows = new ArrayList<>();
        for (VoiceEvent event : EventCatalog.all()) {
            List<VoiceLine> lines = voice.voice().lines(event);
            if (lines.isEmpty()) {
                continue;
            }
            rows.add(new RowList.RowBuilder(font).line(Component.literal(event.id()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).build());
            for (int i = 0; i < lines.size(); i++) {
                VoiceLine line = lines.get(i);
                String file = line.file().substring(line.file().lastIndexOf('/') + 1);
                String details = file + line.duration().map(d -> String.format(Locale.ROOT, " · %.1f s", d)).orElse("") + conditions(line.conditions());
                rows.add(new RowList.RowBuilder(font)
                        .line(Component.literal("#" + i + " " + line.text().orElse("")))
                        .secondary(Component.literal(details))
                        .widget(Button.builder(Component.literal("▶"), button -> PreviewSounds.play(pack, line, 1.0f)).width(20).build())
                        .build());
            }
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

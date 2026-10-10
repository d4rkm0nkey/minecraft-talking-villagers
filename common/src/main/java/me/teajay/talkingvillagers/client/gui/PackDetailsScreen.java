package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.client.PreviewSounds;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.Problem;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.voice.VoiceCommandService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The voices of a pack, with sample playback, or the problems of a broken pack.
 */
public class PackDetailsScreen extends ListScreen {
    private static final int RED = 0xFFFF5555;
    private static final int YELLOW = 0xFFFFFF55;
    private final LoadedPack pack;
    private final Random random = new Random();

    public PackDetailsScreen(Screen parent, LoadedPack pack) {
        super(parent, Component.literal(pack.pack().map(VoicePack::name).orElse(pack.source().getFileName().toString())));
        this.pack = pack;
    }

    @Override
    protected boolean searchable() {
        return pack.pack().map(p -> p.voices().size() > 3).orElse(false);
    }

    @Override
    protected List<RowList.Row> rows() {
        List<RowList.Row> rows = new ArrayList<>();
        if (pack.pack().isPresent()) {
            VoicePack p = pack.pack().get();
            rows.add(new RowList.RowBuilder(font).icon(PackIcons.get(pack))
                    .line(GuiTexts.key("details.info", p.version(), p.language(), p.voices().size()))
                    .secondary(p.authors().isEmpty() ? Component.literal(pack.source().getFileName().toString())
                            : GuiTexts.key("details.authors", String.join(", ", p.authors())))
                    .secondary(Component.literal(p.description().orElse("")))
                    .build());
            boolean editable = GuiTexts.canEditServerSettings();
            for (ResolvedVoice voice : p.resolvedVoices()) {
                if (matches(voice.voice().name(), voice.ref().voiceId(), GuiTexts.entities(voice.voice()).getString(), voice.language())) {
                    rows.add(voiceRow(voice, editable));
                }
            }
        }
        for (Problem problem : pack.problems()) {
            rows.add(new RowList.RowBuilder(font)
                    .line(Component.literal((problem.isError() ? "✖ " : "⚠ ") + problem.message()), problem.isError() ? RED : YELLOW)
                    .secondary(Component.literal(problem.path().isEmpty() ? problem.code().id() : problem.path()))
                    .build());
        }
        return rows;
    }

    private RowList.Row voiceRow(ResolvedVoice resolved, boolean editable) {
        Voice voice = resolved.voice();
        boolean enabled = Configs.server().isVoiceEnabled(resolved.ref());
        int lines = voice.lines().values().stream().mapToInt(List::size).sum();

        Button play = Button.builder(Component.literal("▶"), button -> playSample(resolved)).width(20)
                .tooltip(Tooltip.create(GuiTexts.key("details.play"))).build();
        play.active = VoiceCommandService.sampleEvent(resolved).isPresent();
        Button toggle = Button.builder(enabled ? GuiTexts.key("packs.enabled") : GuiTexts.key("packs.disabled"), button -> {
            Configs.setServer(Configs.server().withVoiceEnabled(resolved.ref(), !enabled));
            refresh();
        }).width(50).build();
        toggle.active = editable;
        if (!editable) {
            toggle.setTooltip(Tooltip.create(GuiTexts.key("server_controlled")));
        }

        Component name = Component.literal(voice.name() + " ")
                .append(GuiTexts.gender(voice.gender()).copy().withStyle(ChatFormatting.GRAY))
                .append(voice.child() ? Component.literal(" ").append(GuiTexts.key("child").copy().withStyle(ChatFormatting.AQUA)) : Component.empty());
        return new RowList.RowBuilder(font)
                .line(name, enabled ? 0xFFFFFFFF : 0xFF808080)
                .secondary(GuiTexts.entities(voice).copy().append(" · " + resolved.language() + " · ").append(GuiTexts.key("details.lines", lines)))
                .widget(play)
                .widget(Button.builder(GuiTexts.key("details.show_lines"), button -> minecraft.gui.setScreen(new VoiceEventsScreen(this, pack, resolved)))
                        .width(56).build())
                .widget(toggle)
                .build();
    }

    private void playSample(ResolvedVoice voice) {
        VoiceCommandService.sampleEvent(voice).ifPresent(event -> {
            List<VoiceLine> lines = voice.voice().lines(event);
            PreviewSounds.play(pack, lines.get(random.nextInt(lines.size())), voice.voice().pitch().sample(ThreadLocalRandom.current()));
        });
    }
}

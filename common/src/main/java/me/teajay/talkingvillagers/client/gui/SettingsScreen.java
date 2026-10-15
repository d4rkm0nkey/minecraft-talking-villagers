package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.config.ClientConfig;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.config.ServerConfig;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.language.LanguageMode;
import me.teajay.talkingvillagers.core.language.LanguageSettings;
import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Settings of the mod. Changes are saved when the screen is closed. Settings that decide which voices entities get
 * belong to the world's server and can only be changed in singleplayer or in the main menu.
 */
public class SettingsScreen extends ListScreen {
    private static final int WIDGET_WIDTH = 150;
    private ClientConfig client = Configs.client();
    private ServerConfig server = Configs.server();

    public SettingsScreen(Screen parent) {
        super(parent, GuiTexts.key("settings.title"));
    }

    @Override
    protected List<RowList.Row> rows() {
        boolean editable = GuiTexts.canEditServerSettings();
        List<RowList.Row> rows = new ArrayList<>();

        section(rows, "settings.client");
        row(rows, "settings.volume", slider(0, 2, client.volume(), value -> GuiTexts.key("settings.percent", Math.round(value * 100)),
                value -> client = client.withVolume((float) value)));
        row(rows, "settings.subtitles", CycleButton.<ClientConfig.Subtitles>builder(mode -> GuiTexts.key("settings.subtitles." + mode.id()), client.subtitles())
                .withValues(ClientConfig.Subtitles.values()).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> client = client.withSubtitles(value)));

        section(rows, "settings.voices");
        if (!editable) {
            rows.add(new RowList.RowBuilder(font).secondary(GuiTexts.key("server_controlled")).build());
        }
        row(rows, "settings.female_chance", serverSlider(0, 1, server.femaleChance(), value -> GuiTexts.key("settings.percent", Math.round(value * 100)),
                value -> server = server.withFemaleChance(value), editable));
        row(rows, "settings.witch_female_chance", serverSlider(0, 1, server.witchFemaleChance(), value -> GuiTexts.key("settings.percent", Math.round(value * 100)),
                value -> server = server.withWitchFemaleChance(value), editable));
        row(rows, "settings.child_fallback", active(CycleButton.<VoiceCriteria.ChildFallback>builder(
                        value -> GuiTexts.key("settings.child_fallback." + value.name().toLowerCase(Locale.ROOT)), server.childFallback())
                .withValues(VoiceCriteria.ChildFallback.values()).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withChildFallback(value)), editable));
        row(rows, "settings.max_speakers", serverSlider(1, 10, server.maxConcurrentSpeakers(), value -> Component.literal(Integer.toString((int) Math.round(value))),
                value -> server = server.withMaxConcurrentSpeakers((int) Math.round(value)), editable));

        section(rows, "settings.entities");
        for (EntityKind kind : EntityKind.values()) {
            rows.add(new RowList.RowBuilder(font)
                    .line(Component.translatable("entity.minecraft." + kind.id()))
                    .widget(active(CycleButton.onOffBuilder(server.isEnabled(kind))
                            .displayOnlyValue()
                            .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withEntityEnabled(kind, value)), editable))
                    .build());
        }

        section(rows, "settings.language");
        row(rows, "settings.language_mode", active(CycleButton.<LanguageMode>builder(mode -> GuiTexts.key("settings.language_mode." + mode.id()), server.languageMode())
                .withValues(LanguageMode.values()).displayOnlyValue()
                .withTooltip(mode -> Tooltip.create(GuiTexts.key("settings.language_mode." + mode.id() + ".tooltip")))
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withLanguageMode(value)), editable));
        row(rows, "settings.preferred_languages", languageBox(server.preferredLanguages(), value -> server = server.withPreferredLanguages(value), editable));
        row(rows, "settings.allowed_languages", languageBox(server.allowedLanguages(), value -> server = server.withAllowedLanguages(value), editable));
        row(rows, "settings.strict_language", active(CycleButton.onOffBuilder(server.strictLanguage()).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withStrictLanguage(value)), editable));
        row(rows, "settings.trader_language", active(CycleButton.<LanguageSettings.TraderLanguage>builder(
                        value -> GuiTexts.key("settings.trader_language." + value.id()), server.traderLanguage())
                .withValues(LanguageSettings.TraderLanguage.values()).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withTraderLanguage(value)), editable));
        row(rows, "settings.raid_language", active(CycleButton.<LanguageSettings.RaidLanguage>builder(
                        value -> GuiTexts.key("settings.raid_language." + value.id()), server.raidLanguage())
                .withValues(LanguageSettings.RaidLanguage.values()).displayOnlyValue()
                .create(0, 0, WIDGET_WIDTH, 20, Component.empty(), (button, value) -> server = server.withRaidLanguage(value)), editable));
        return rows;
    }

    @Override
    public void onClose() {
        Configs.setClient(client);
        if (GuiTexts.canEditServerSettings() && !server.equals(Configs.server())) {
            Configs.setServer(server);
        }
        super.onClose();
    }

    private void section(List<RowList.Row> rows, String key) {
        rows.add(new RowList.RowBuilder(font).line(GuiTexts.key(key).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).build());
    }

    private void row(List<RowList.Row> rows, String key, AbstractWidget widget) {
        rows.add(new RowList.RowBuilder(font).line(GuiTexts.key(key)).widget(widget).build());
    }

    private static <T extends AbstractWidget> T active(T widget, boolean active) {
        widget.active = active;
        return widget;
    }

    private AbstractWidget serverSlider(double min, double max, double value, DoubleFunction<Component> label, DoubleConsumer onChange, boolean editable) {
        return active(slider(min, max, value, label, onChange), editable);
    }

    private static AbstractWidget slider(double min, double max, double value, DoubleFunction<Component> label, DoubleConsumer onChange) {
        return new AbstractSliderButton(0, 0, WIDGET_WIDTH, 20, label.apply(value), (value - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                setMessage(label.apply(min + this.value * (max - min)));
            }

            @Override
            protected void applyValue() {
                onChange.accept(min + this.value * (max - min));
            }
        };
    }

    /** Comma separated language tags; invalid tags are ignored. */
    private EditBox languageBox(List<String> languages, Consumer<List<String>> onChange, boolean editable) {
        EditBox box = new EditBox(font, 0, 0, WIDGET_WIDTH, 18, Component.empty());
        box.setMaxLength(200);
        box.setValue(String.join(", ", languages));
        box.setHint(GuiTexts.key("settings.languages.hint").copy().withStyle(EditBox.SEARCH_HINT_STYLE));
        box.setResponder(value -> onChange.accept(Arrays.stream(value.split("[,\\s]+"))
                .filter(Languages::isValid).map(Languages::normalize).distinct().toList()));
        box.active = editable;
        box.setEditable(editable);
        return box;
    }
}

package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.convert.Version1Converter;
import me.teajay.talkingvillagers.core.io.ZipPackContents;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.util.SafeFiles;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts a version 1 resource pack. Version 1 did not know languages and genders, so the player sets them here.
 */
class ConvertScreen extends Screen {
    private final Screen parent;
    private final Path file;
    private final Runnable afterConvert;
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private String name;
    private String language;
    private Gender gender = Gender.NEUTRAL;
    private Button convert;

    ConvertScreen(Screen parent, Path file, Runnable afterConvert) {
        super(GuiTexts.key("convert.title"));
        this.parent = parent;
        this.file = file;
        this.afterConvert = afterConvert;
        this.name = file.getFileName().toString().replaceAll("\\.zip$", "");
        this.language = Locale.getDefault().getLanguage().isEmpty() ? "en" : Locale.getDefault().getLanguage();
    }

    @Override
    protected void init() {
        layout = new HeaderAndFooterLayout(this);
        layout.addTitleHeader(title, font);
        LinearLayout content = layout.addToContents(LinearLayout.vertical().spacing(6));
        content.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(new StringWidget(Component.literal(file.getFileName().toString()).withStyle(ChatFormatting.GRAY), font));
        content.addChild(new StringWidget(GuiTexts.key("convert.name"), font));
        EditBox nameBox = content.addChild(new EditBox(font, 0, 0, 200, 18, GuiTexts.key("convert.name")));
        nameBox.setValue(name);
        nameBox.setResponder(value -> {
            name = value;
            updateButton();
        });
        content.addChild(new StringWidget(GuiTexts.key("convert.language"), font));
        EditBox languageBox = content.addChild(new EditBox(font, 0, 0, 200, 18, GuiTexts.key("convert.language")));
        languageBox.setValue(language);
        languageBox.setResponder(value -> {
            language = value.trim();
            updateButton();
        });
        content.addChild(CycleButton.<Gender>builder(GuiTexts::gender, gender).withValues(Gender.values())
                .create(0, 0, 200, 20, GuiTexts.key("convert.gender"), (button, value) -> gender = value));

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        convert = footer.addChild(Button.builder(GuiTexts.key("convert.start"), button -> convert()).width(120).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(120).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        updateButton();
    }

    private void updateButton() {
        if (convert != null) {
            convert.active = !name.isBlank() && Languages.isValid(language);
        }
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private void convert() {
        String id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (id.isEmpty()) {
            id = "converted";
        }
        // never overwrite another pack that happens to have the same name
        Path target = SafeFiles.freeName(Voicepacks.repository().directory().resolve(id + VoicePack.FILE_EXTENSION));
        try (ZipPackContents contents = new ZipPackContents(file)) {
            Version1Converter.Result result = Version1Converter.convertResourcePack(contents,
                    new Version1Converter.Settings(id, name.trim(), "2.0.0", Languages.normalize(language), gender, List.of()));
            Version1Converter.write(result, Map.of(), target);
            Voicepacks.reload();
            afterConvert.run();
            minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(parent), GuiTexts.key("convert.done"),
                    GuiTexts.key("convert.done.message", result.pack().voices().size(), result.files().size(), result.skipped().size())));
        } catch (Exception e) {
            TalkingVillagers.LOG.error("Cannot convert {}", file, e);
            minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(parent), GuiTexts.key("convert.failed"),
                    Component.literal(String.valueOf(e.getMessage()))));
        }
    }
}

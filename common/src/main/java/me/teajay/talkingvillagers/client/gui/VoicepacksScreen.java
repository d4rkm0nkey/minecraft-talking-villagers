package me.teajay.talkingvillagers.client.gui;

import com.mojang.blaze3d.Blaze3D;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.client.ClientVoices;
import me.teajay.talkingvillagers.client.PackDownloads;
import me.teajay.talkingvillagers.client.PreviewSounds;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The voicepack manager: lists installed packs, installs dropped files, enables, disables and deletes packs.
 */
public class VoicepacksScreen extends Screen {
    public static final URI WEB_EDITOR = URI.create("https://d4rkm0nkey.github.io/minecraft-talking-villagers/editor/");
    private static final int RED = 0xFFFF5555;
    private final @Nullable Screen parent;
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 56, 33);
    private @Nullable RowList list;
    private @Nullable EditBox search;
    private boolean downloadsShown;
    /** A reload or file operation runs in the background. */
    private boolean loading;
    private @Nullable Button reloadButton;
    private int ticks;

    public VoicepacksScreen(@Nullable Screen parent) {
        super(GuiTexts.key("packs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout = new HeaderAndFooterLayout(this, 56, 33);
        LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title, font));
        header.addChild(new StringWidget(GuiTexts.key("packs.drop").copy().withStyle(ChatFormatting.GRAY), font));
        search = header.addChild(new EditBox(font, 0, 0, 200, 15, GuiTexts.key("packs.search")));
        search.setHint(GuiTexts.key("packs.search").copy().withStyle(EditBox.SEARCH_HINT_STYLE));
        search.setResponder(value -> refresh());

        list = layout.addToContents(new RowList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight()));

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(6));
        footer.addChild(Button.builder(GuiTexts.key("packs.open_folder"), button -> Blaze3D.openPath(Voicepacks.repository().directory()))
                .width(76).tooltip(Tooltip.create(GuiTexts.key("packs.open_folder.tooltip"))).build());
        reloadButton = footer.addChild(Button.builder(GuiTexts.key("packs.reload"), button -> changePacks(() -> {
        }, () -> {
        })).width(76).build());
        footer.addChild(Button.builder(GuiTexts.key("packs.web_editor"), ConfirmLinkScreen.confirmLink(this, WEB_EDITOR))
                .width(76).tooltip(Tooltip.create(GuiTexts.key("packs.web_editor.tooltip"))).build());
        footer.addChild(Button.builder(GuiTexts.key("packs.settings"), button -> minecraft.gui.setScreen(new SettingsScreen(this))).width(76).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(76).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        refresh();
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

    @Override
    public void onFilesDrop(List<Path> files) {
        if (!loading) {
            PackInstaller.install(this, files);
        }
    }

    /** A file operation that changes the packs, run before the reload. */
    @FunctionalInterface
    interface PackChange {
        void run() throws IOException;
    }

    /**
     * Runs {@code change} and the reload in the background; meanwhile the screen says so and nothing else can change
     * the packs. {@code afterReload} runs on the client thread at the end.
     */
    void changePacks(PackChange change, Runnable afterReload) {
        loading = true;
        refresh();
        CompletableFuture.runAsync(() -> {
                    try {
                        change.run();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, Util.ioPool())
                .exceptionally(error -> {
                    TalkingVillagers.LOG.error("Cannot change voicepacks", error);
                    return null;
                })
                .thenCompose(ignored -> Voicepacks.reloadAsync())
                .whenCompleteAsync((index, error) -> {
                    loading = false;
                    afterReload.run();
                    refresh();
                }, minecraft);
    }

    void refresh() {
        if (list == null) {
            return;
        }
        PackIndex index = Voicepacks.index();
        String filter = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        boolean editable = GuiTexts.canEditServerSettings();
        List<RowList.Row> rows = new ArrayList<>();
        if (reloadButton != null) {
            reloadButton.active = !loading;
        }
        if (loading) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.loading").copy().withStyle(ChatFormatting.YELLOW)).centered().build());
            editable = false;
        }
        Feedback.addRow(rows, this, font);

        if (index.all().isEmpty()) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.empty")).centered().build());
            rows.add(new RowList.RowBuilder(font).secondary(GuiTexts.key("packs.empty.hint")).centered().build());
        }
        List<LoadedPack> packs = index.all().stream()
                .sorted(Comparator.comparing((LoadedPack pack) -> !pack.isUsable())
                        .thenComparing(pack -> pack.pack().map(VoicePack::name).orElse(pack.source().getFileName().toString()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (LoadedPack pack : packs) {
            if (matches(pack, filter)) {
                rows.add(pack.isUsable() ? packRow(pack, index, editable) : brokenRow(pack));
            }
        }

        if (hasVersion1Config()) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.v1_found").copy().withStyle(ChatFormatting.YELLOW))
                    .secondary(GuiTexts.key("packs.v1_found.hint")).build());
        }

        if (ClientVoices.isServerWithoutMod()) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.server_without_mod").copy().withStyle(ChatFormatting.YELLOW))
                    .secondary(GuiTexts.key("packs.server_without_mod.hint")).build());
        }

        List<ServerPacksPayload.PackInfo> missing = ClientVoices.missingPacks();
        if (!missing.isEmpty()) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.missing").copy().withStyle(ChatFormatting.YELLOW)).build());
            Map<String, PackDownloads.Download> downloads = PackDownloads.active();
            for (ServerPacksPayload.PackInfo info : missing) {
                PackDownloads.Download download = downloads.get(info.sha256());
                RowList.RowBuilder row = new RowList.RowBuilder(font).icon(PackIcons.DEFAULT)
                        .line(Component.literal(info.name() + " " + info.version()));
                if (download != null) {
                    row.secondary(download.progress() < 0 ? GuiTexts.key("packs.downloading")
                            : GuiTexts.key("packs.downloading.progress", Math.round(download.progress() * 100)));
                } else {
                    row.secondary(GuiTexts.key("packs.missing.entry", GuiTexts.size(info.size())));
                    if (PackDownloads.canDownload(info)) {
                        row.widget(Button.builder(GuiTexts.key("packs.download"), button -> {
                            PackDownloads.accept(List.of(info), false);
                            refresh();
                        }).width(80).build());
                    }
                }
                rows.add(row.build());
            }
        }

        if (!index.cached().isEmpty()) {
            rows.add(new RowList.RowBuilder(font).line(GuiTexts.key("packs.cached").copy().withStyle(ChatFormatting.GRAY))
                    .secondary(GuiTexts.key("packs.cached.hint")).build());
            for (LoadedPack cached : index.cached()) {
                if (cached.isUsable() && matches(cached, filter)) {
                    rows.add(cachedRow(cached, index));
                }
            }
        }
        double scroll = list.scrollAmount();
        list.setRows(rows);
        // keep the place in the list, e.g. while download progress updates every half second
        list.setScrollAmount(scroll);
        downloadsShown = !PackDownloads.active().isEmpty();
    }

    @Override
    public void tick() {
        // show download progress and finished downloads
        if ((downloadsShown || !PackDownloads.active().isEmpty()) && ++ticks % 10 == 0) {
            refresh();
        }
    }

    private RowList.Row cachedRow(LoadedPack loaded, PackIndex index) {
        VoicePack pack = loaded.pack().orElseThrow();
        boolean installed = index.all().stream().anyMatch(other -> other.sha256().equals(loaded.sha256()));
        Button install = Button.builder(GuiTexts.key("packs.install_cached"),
                button -> changePacks(() -> Voicepacks.repository().installCached(loaded), () -> {
                })).width(110).tooltip(Tooltip.create(GuiTexts.key("packs.install_cached.tooltip"))).build();
        install.active = !installed && !loading;
        return new RowList.RowBuilder(font)
                .icon(PackIcons.get(loaded))
                .line(Component.literal(pack.name() + " ").append(Component.literal(pack.version()).withStyle(ChatFormatting.GRAY)), 0xFFA0A0A0)
                .secondary(GuiTexts.key("packs.entry", pack.language(), pack.voices().size()))
                .widget(Button.builder(GuiTexts.key("packs.details"), button -> minecraft.gui.setScreen(new PackDetailsScreen(this, loaded))).width(56).build())
                .widget(install)
                .build();
    }

    /** Version 1 kept one JSON file per voice in config/talkingvillagers/voices. */
    private static boolean hasVersion1Config() {
        Path voices = Services.PLATFORM.getGameDir().resolve("config").resolve("talkingvillagers").resolve("voices");
        try (var files = Files.list(voices)) {
            return files.anyMatch(file -> file.toString().endsWith(".json"));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean matches(LoadedPack pack, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        String text = pack.source().getFileName() + " " + pack.pack().map(p -> p.id() + " " + p.name() + " " + p.language()).orElse("");
        return text.toLowerCase(Locale.ROOT).contains(filter);
    }

    private RowList.Row packRow(LoadedPack loaded, PackIndex index, boolean editable) {
        VoicePack pack = loaded.pack().orElseThrow();
        boolean enabled = Configs.server().isPackEnabled(pack.id());
        boolean shadowed = enabled && index.active().get(pack.id()) != loaded;

        Button toggle = Button.builder(enabled ? GuiTexts.key("packs.enabled") : GuiTexts.key("packs.disabled"), button -> {
            Configs.setServer(Configs.server().withPackEnabled(pack.id(), !enabled));
            refresh();
        }).width(50).build();
        toggle.active = editable;
        if (!editable) {
            toggle.setTooltip(Tooltip.create(GuiTexts.key("server_controlled")));
        }
        Button delete = Button.builder(Component.literal("🗑"), button -> confirmDelete(loaded, pack.name()))
                .width(20).tooltip(Tooltip.create(GuiTexts.key("packs.delete"))).build();
        delete.active = !loading;

        Component status = shadowed ? GuiTexts.key("packs.shadowed") : Component.empty();
        return new RowList.RowBuilder(font)
                .icon(PackIcons.get(loaded))
                .line(Component.literal(pack.name() + " ").append(Component.literal(pack.version()).withStyle(ChatFormatting.GRAY)),
                        enabled && !shadowed ? 0xFFFFFFFF : 0xFF808080)
                .secondary(GuiTexts.key("packs.entry", pack.language(), pack.voices().size()).copy().append(" – ").append(GuiTexts.summary(pack)).append(status))
                .widget(Button.builder(GuiTexts.key("packs.details"), button -> minecraft.gui.setScreen(new PackDetailsScreen(this, loaded))).width(56).build())
                .widget(toggle)
                .widget(delete)
                .build();
    }

    private RowList.Row brokenRow(LoadedPack loaded) {
        String file = loaded.source().getFileName().toString();
        return new RowList.RowBuilder(font)
                .icon(PackIcons.DEFAULT)
                .line(Component.literal(file), RED)
                .secondary(GuiTexts.key("packs.broken", loaded.errors().size(), loaded.errors().getFirst().message()))
                .widget(Button.builder(GuiTexts.key("packs.details"), button -> minecraft.gui.setScreen(new PackDetailsScreen(this, loaded))).width(56).build())
                .widget(active(Button.builder(Component.literal("🗑"), button -> confirmDelete(loaded, file)).width(20)
                        .tooltip(Tooltip.create(GuiTexts.key("packs.delete"))).build(), !loading))
                .build();
    }

    private void confirmDelete(LoadedPack pack, String name) {
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            minecraft.gui.setScreen(this);
            if (confirmed) {
                PackInstaller.delete(this, pack);
            }
        }, GuiTexts.key("packs.delete.confirm", name), GuiTexts.key("packs.delete.message")));
    }

    private static Button active(Button button, boolean active) {
        button.active = active;
        return button;
    }
}

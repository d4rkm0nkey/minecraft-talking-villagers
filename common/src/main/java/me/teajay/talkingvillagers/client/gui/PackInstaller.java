package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.util.SafeFiles;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;

/**
 * Installs dropped {@code .voicepack} files and deletes packs.
 */
final class PackInstaller {
    private static final String V1_SOUND_DIR = "assets/talkingvillagers/sounds/voices/";
    private static final String TRASH = ".trash";

    private PackInstaller() {
    }

    private record Candidate(Path file, LoadedPack pack, Optional<LoadedPack> replaces) {
    }

    private record Check(List<Candidate> candidates, List<String> rejected, boolean convert) {
    }

    /**
     * Checks the files in the background, asks for confirmation and copies the valid packs into the voicepacks folder.
     */
    static void install(VoicepacksScreen screen, List<Path> files) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new GenericMessageScreen(GuiTexts.key("install.checking")));
        CompletableFuture.supplyAsync(() -> check(files), Util.ioPool())
                .whenCompleteAsync((check, error) -> {
                    if (error != null) {
                        TalkingVillagers.LOG.error("Cannot check {}", files, error);
                        minecraft.gui.setScreen(screen);
                    } else {
                        confirm(screen, files, check);
                    }
                }, minecraft);
    }

    /** Background thread: reads and validates the dropped files. */
    private static Check check(List<Path> files) {
        List<Candidate> candidates = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            if (!name.endsWith(VoicePack.FILE_EXTENSION)) {
                boolean version1 = isVersion1Pack(file);
                if (files.size() == 1 && version1) {
                    // an old resource pack: offer to convert it
                    return new Check(List.of(), List.of(), true);
                }
                rejected.add(Component.translatable(version1 ? "talkingvillagers.gui.install.v1" : "talkingvillagers.gui.install.not_voicepack", name).getString());
                continue;
            }
            LoadedPack pack = PackReader.read(file);
            if (!pack.isUsable()) {
                rejected.add(name + ": " + pack.errors().getFirst().message());
                continue;
            }
            String id = pack.pack().orElseThrow().id();
            candidates.add(new Candidate(file, pack, Optional.ofNullable(Voicepacks.index().active().get(id))));
        }
        return new Check(candidates, rejected, false);
    }

    private static void confirm(VoicepacksScreen screen, List<Path> files, Check check) {
        Minecraft minecraft = Minecraft.getInstance();
        if (check.convert()) {
            minecraft.gui.setScreen(new ConvertScreen(screen, files.getFirst()));
            return;
        }
        List<String> rejected = check.rejected();
        if (check.candidates().isEmpty()) {
            minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(screen), GuiTexts.key("install.rejected"), Component.literal(String.join("\n", rejected))));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Candidate candidate : check.candidates()) {
            VoicePack pack = candidate.pack().pack().orElseThrow();
            String line = Component.translatable("talkingvillagers.gui.install.entry", pack.name(), pack.version(), pack.voices().size(), pack.language()).getString();
            if (candidate.replaces().isPresent()) {
                line += " " + Component.translatable("talkingvillagers.gui.install.replaces", candidate.replaces().get().pack().orElseThrow().version()).getString();
            }
            lines.add(line);
        }
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed && !rejected.isEmpty()) {
                minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(screen), GuiTexts.key("install.rejected"), Component.literal(String.join("\n", rejected))));
            } else {
                minecraft.gui.setScreen(screen);
            }
            if (confirmed) {
                int[] installed = new int[1];
                screen.changePacks(() -> installed[0] = copy(check.candidates()), () -> SystemToast.add(minecraft.gui.toastManager(),
                        SystemToast.SystemToastId.PACK_LOAD_FAILURE, GuiTexts.key("install.done", installed[0]), null));
            }
        }, GuiTexts.key("install.confirm"), Component.literal(String.join("\n", lines))));
    }

    /** Background thread. @return how many packs were installed */
    private static int copy(List<Candidate> candidates) {
        Path directory = Voicepacks.repository().directory();
        int installed = 0;
        for (Candidate candidate : candidates) {
            Path target = directory.resolve(candidate.file().getFileName());
            Optional<LoadedPack> old = candidate.replaces();
            try {
                Files.createDirectories(directory);
                if (!candidate.file().toAbsolutePath().equals(target.toAbsolutePath())) {
                    // only the older version of the same pack is replaced, any other file with this name stays
                    boolean replacesOld = old.isPresent() && old.get().source().toAbsolutePath().equals(target.toAbsolutePath());
                    if (!replacesOld) {
                        target = SafeFiles.freeName(target);
                    }
                    SafeFiles.copyAtomically(candidate.file(), target);
                }
                // an update replaces the older file of the same pack
                if (old.isPresent() && !old.get().source().toAbsolutePath().equals(target.toAbsolutePath())) {
                    moveToTrash(old.get().source());
                }
                installed++;
            } catch (IOException e) {
                TalkingVillagers.LOG.error("Cannot install {}", candidate.file(), e);
            }
        }
        return installed;
    }

    /**
     * Moves a pack file into {@code voicepacks/.trash}, so it can be restored by hand.
     */
    static void delete(VoicepacksScreen screen, LoadedPack pack) {
        screen.changePacks(() -> moveToTrash(pack.source()), () -> {
        });
    }

    private static void moveToTrash(Path file) throws IOException {
        Path trash = Voicepacks.repository().directory().resolve(TRASH);
        Files.createDirectories(trash);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Files.move(file, trash.resolve(stamp + "-" + file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean isVersion1Pack(Path file) {
        if (!file.getFileName().toString().endsWith(".zip")) {
            return false;
        }
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return zip.stream().anyMatch(entry -> entry.getName().startsWith(V1_SOUND_DIR));
        } catch (IOException e) {
            return false;
        }
    }
}

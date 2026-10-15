package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    /**
     * Checks the files, asks for confirmation and copies the valid packs into the voicepacks folder.
     */
    static void install(Screen screen, List<Path> files, Runnable afterInstall) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Candidate> candidates = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            if (!name.endsWith(VoicePack.FILE_EXTENSION)) {
                if (files.size() == 1 && isVersion1Pack(file)) {
                    // an old resource pack: offer to convert it
                    minecraft.gui.setScreen(new ConvertScreen(screen, file, afterInstall));
                    return;
                }
                rejected.add(Component.translatable(isVersion1Pack(file) ? "talkingvillagers.gui.install.v1" : "talkingvillagers.gui.install.not_voicepack", name).getString());
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

        if (candidates.isEmpty()) {
            minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(screen), GuiTexts.key("install.rejected"), Component.literal(String.join("\n", rejected))));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Candidate candidate : candidates) {
            VoicePack pack = candidate.pack().pack().orElseThrow();
            String line = Component.translatable("talkingvillagers.gui.install.entry", pack.name(), pack.version(), pack.voices().size(), pack.language()).getString();
            if (candidate.replaces().isPresent()) {
                line += " " + Component.translatable("talkingvillagers.gui.install.replaces", candidate.replaces().get().pack().orElseThrow().version()).getString();
            }
            lines.add(line);
        }
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                copy(candidates);
                afterInstall.run();
            }
            if (confirmed && !rejected.isEmpty()) {
                minecraft.gui.setScreen(new AlertScreen(() -> minecraft.gui.setScreen(screen), GuiTexts.key("install.rejected"), Component.literal(String.join("\n", rejected))));
            } else {
                minecraft.gui.setScreen(screen);
            }
        }, GuiTexts.key("install.confirm"), Component.literal(String.join("\n", lines))));
    }

    private static void copy(List<Candidate> candidates) {
        Path directory = Voicepacks.repository().directory();
        int installed = 0;
        for (Candidate candidate : candidates) {
            Path target = directory.resolve(candidate.file().getFileName());
            try {
                Files.createDirectories(directory);
                if (!candidate.file().toAbsolutePath().equals(target.toAbsolutePath())) {
                    Files.copy(candidate.file(), target, StandardCopyOption.REPLACE_EXISTING);
                }
                // an update replaces the older file of the same pack
                Optional<LoadedPack> old = candidate.replaces();
                if (old.isPresent() && !old.get().source().toAbsolutePath().equals(target.toAbsolutePath())) {
                    moveToTrash(old.get().source());
                }
                installed++;
            } catch (IOException e) {
                TalkingVillagers.LOG.error("Cannot install {}", candidate.file(), e);
            }
        }
        Voicepacks.reload();
        SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                GuiTexts.key("install.done", installed), null);
    }

    /**
     * Moves a pack file into {@code voicepacks/.trash}, so it can be restored by hand.
     */
    static boolean delete(LoadedPack pack) {
        try {
            moveToTrash(pack.source());
            Voicepacks.reload();
            return true;
        } catch (IOException e) {
            TalkingVillagers.LOG.error("Cannot delete {}", pack.source(), e);
            return false;
        }
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

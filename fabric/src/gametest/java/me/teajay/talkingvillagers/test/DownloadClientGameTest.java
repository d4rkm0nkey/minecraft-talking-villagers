package me.teajay.talkingvillagers.test;

import me.teajay.talkingvillagers.client.ClientVoices;
import me.teajay.talkingvillagers.client.PackDownloads;
import me.teajay.talkingvillagers.client.gui.DownloadPromptScreen;
import me.teajay.talkingvillagers.client.gui.VoicepacksScreen;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Joins a real dedicated server with voicepacks this client does not have and downloads them through the game
 * connection, once interrupted by a disconnect and once completely.
 *
 * <p>Only runs when the environment variable {@code TV_TEST_SERVER} names the server, e.g. {@code localhost:25565}.
 * The server needs {@code online-mode=false} and at least one voicepack.
 */
public class DownloadClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String address = System.getenv("TV_TEST_SERVER");
        if (address == null || address.isBlank()) {
            return;
        }
        Path cache = Voicepacks.repository().cacheDirectory();
        deleteRecursively(cache);
        context.runOnClient(client -> Voicepacks.reload());

        // 1. interrupted download leaves nothing behind
        connect(context, address);
        context.waitFor(client -> client.gui.screen() instanceof DownloadPromptScreen, 600);
        context.takeScreenshot("tv-download-prompt");
        context.runOnClient(client -> {
            PackDownloads.accept(ClientVoices.missingPacks(), false);
            client.gui.setScreen(null);
        });
        context.waitFor(client -> PackDownloads.active().values().stream().anyMatch(download -> download.progress() > 0), 600);
        disconnect(context);
        if (!listFiles(cache).isEmpty() || !Voicepacks.index().cached().isEmpty()) {
            throw new AssertionError("Interrupted download left files: " + listFiles(cache));
        }

        // 2. complete download
        connect(context, address);
        context.waitFor(client -> client.gui.screen() instanceof DownloadPromptScreen, 600);
        context.runOnClient(client -> {
            PackDownloads.accept(ClientVoices.missingPacks(), false);
            client.gui.setScreen(null);
        });
        context.waitFor(client -> ClientVoices.missingPacks().isEmpty() && PackDownloads.active().isEmpty(), 2400);
        if (Voicepacks.index().cached().isEmpty()) {
            throw new AssertionError("Nothing in the cache after the download: " + listFiles(cache));
        }
        if (Voicepacks.index().activePacks().stream().anyMatch(pack -> pack.source().startsWith(cache))) {
            throw new AssertionError("A downloaded pack became active");
        }
        context.runOnClient(client -> client.gui.setScreen(new VoicepacksScreen(null)));
        context.waitTicks(10);
        context.takeScreenshot("tv-from-server");
        context.runOnClient(client -> client.gui.setScreen(null));
        disconnect(context);
        deleteRecursively(cache);
        context.runOnClient(client -> Voicepacks.reload());
    }

    private static void connect(ClientGameTestContext context, String address) {
        context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address),
                new ServerData("Test", address, ServerData.Type.OTHER), false, null));
        context.waitFor(client -> client.level != null && client.player != null, 1200);
    }

    private static void disconnect(ClientGameTestContext context) {
        context.runOnClient(client -> client.disconnectFromWorld(Component.empty()));
        // after leaving a multiplayer server vanilla shows the server list
        context.waitFor(client -> client.level == null && client.getConnection() == null, 600);
        context.runOnClient(client -> client.gui.setScreen(new TitleScreen()));
        // the download worker may still be busy with a reload
        context.waitTicks(20);
    }

    private static List<Path> listFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

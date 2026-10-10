package me.teajay.talkingvillagers.test;

import me.teajay.talkingvillagers.client.gui.PackDetailsScreen;
import me.teajay.talkingvillagers.client.gui.VoiceLinesScreen;
import me.teajay.talkingvillagers.client.gui.VoicepacksScreen;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Opens the voicepack manager, a pack, its lines and the settings, and takes a screenshot of each
 * (in the run directory's screenshots folder) to check the layout.
 */
public class GuiClientGameTest implements FabricClientGameTest {
    private static final String MANIFEST = """
            {
              "format": 2, "id": "gui-test", "name": "GUI Test Pack", "version": "1.2.0", "language": "de",
              "description": "Shows every kind of row", "authors": ["TeaJay"],
              "voices": [
                { "id": "anna", "name": "Anna", "entities": ["villager"], "gender": "female",
                  "lines": { "greeting": [{ "file": "sounds/a/hello.ogg", "text": "Hallo, Reisender!" }],
                             "hurt": [{ "file": "sounds/a/hello.ogg" }], "death": [{ "file": "sounds/a/hello.ogg" }] } },
                { "id": "kid", "name": "Kind", "entities": ["villager"], "gender": "male", "child": true,
                  "lines": { "hurt": [{ "file": "sounds/a/hello.ogg" }], "death": [{ "file": "sounds/a/hello.ogg" }] } },
                { "id": "grimm", "name": "Grimm", "entities": ["illager"], "gender": "male",
                  "lines": { "combat": [{ "file": "sounds/a/hello.ogg", "text": "Ich mach dich fertig!" }],
                             "hurt": [{ "file": "sounds/a/hello.ogg" }], "death": [{ "file": "sounds/a/hello.ogg" }] } }
              ]
            }
            """;

    @Override
    public void runTest(ClientGameTestContext context) {
        Path directory = Voicepacks.repository().directory();
        Path pack = directory.resolve("gui-test.voicepack");
        Path broken = directory.resolve("broken.voicepack");
        try {
            Files.createDirectories(directory);
            writePack(pack);
            Files.writeString(broken, "not a zip");
            context.runOnClient(client -> Voicepacks.reload());

            context.setScreen(() -> new VoicepacksScreen(null));
            context.waitTicks(10);
            context.takeScreenshot("tv-voicepacks");

            // buttons inside list rows cannot be clicked by the test API, so the screens are opened directly
            LoadedPack loaded = context.computeOnClient(client -> Voicepacks.index().active().get("gui-test"));
            VoicepacksScreen overview = context.computeOnClient(client -> new VoicepacksScreen(null));
            context.setScreen(() -> new PackDetailsScreen(overview, loaded));
            context.waitTicks(10);
            context.takeScreenshot("tv-details");

            ResolvedVoice grimm = loaded.pack().orElseThrow().resolvedVoices().get(2);
            context.setScreen(() -> new VoiceLinesScreen(overview, loaded, grimm));
            context.waitTicks(10);
            context.takeScreenshot("tv-lines");

            context.setScreen(() -> overview);
            context.waitTicks(5);
            context.clickScreenButton("talkingvillagers.gui.packs.settings");
            context.waitTicks(10);
            context.takeScreenshot("tv-settings");
            context.setScreen(() -> null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(pack);
                Files.deleteIfExists(broken);
            } catch (IOException ignored) {
            }
            context.runOnClient(client -> Voicepacks.reload());
        }
    }

    private static void writePack(Path file) throws IOException {
        byte[] audio;
        try (InputStream in = GuiClientGameTest.class.getResourceAsStream("/audio/three_seconds.ogg")) {
            audio = in.readAllBytes();
        }
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("voicepack.json"));
            zip.write(MANIFEST.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("sounds/a/hello.ogg"));
            zip.write(audio);
            zip.closeEntry();
        }
    }
}

package me.teajay.talkingvillagers.test;

import me.teajay.talkingvillagers.client.ClientVoices;
import me.teajay.talkingvillagers.client.VoiceAudio;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.FiniteAudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

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
 * Installs a voicepack, makes a villager speak in a singleplayer world and checks the line is really played.
 */
public class SpeakClientGameTest implements FabricClientGameTest {
    private static final String LINE = "sounds/anna/greeting/hello.ogg";
    private static final String MANIFEST = """
            {
              "format": 2, "id": "gametest", "name": "Game Test", "version": "1.0.0", "language": "en",
              "voices": [{
                "id": "anna", "name": "Anna", "entities": ["villager"], "gender": "female",
                "lines": { "greeting": [{ "file": "%s", "text": "Hello from the game test" }] }
              }]
            }
            """.formatted(LINE);

    @Override
    public void runTest(ClientGameTestContext context) {
        Path pack = Voicepacks.repository().directory().resolve("gametest.voicepack");
        try {
            writePack(pack);
            context.runOnClient(client -> Voicepacks.reload());
            LoadedPack loaded = Voicepacks.index().active().get("gametest");
            if (loaded == null) {
                throw new AssertionError("Pack not loaded: " + Voicepacks.index().all());
            }

            assertDecodes(loaded.sha256());

            try (TestSingleplayerContext world = context.worldBuilder().create()) {
                world.getServer().runCommand("summon minecraft:villager ~2 ~ ~ {NoAI:1b,Tags:[tvtest]}");
                context.waitTicks(20);
                world.getServer().runCommand("tv debug say @e[tag=tvtest] gametest:anna greeting");

                context.waitFor(SpeakClientGameTest::anyVillagerSpeaking, 100);
                // the test sound is three seconds long; if its buffer failed to load the engine drops it after one second
                context.waitTicks(40);
                boolean stillSpeaking = context.computeOnClient(SpeakClientGameTest::anyVillagerSpeaking);
                if (!stillSpeaking) {
                    throw new AssertionError("The voice line stopped early, the audio was probably not loaded");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(pack);
            } catch (IOException ignored) {
            }
            context.runOnClient(client -> Voicepacks.reload());
        }
    }

    private static boolean anyVillagerSpeaking(Minecraft client) {
        if (client.level == null) {
            return false;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Villager && ClientVoices.isSpeaking(entity.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Decodes the line through the same path the sound engine uses. */
    private static void assertDecodes(String packHash) throws IOException {
        var file = Sound.SOUND_LISTER.idToFile(VoiceAudio.soundLocation(packHash, LINE));
        try (InputStream in = VoiceAudio.open(file).orElseThrow(); FiniteAudioStream stream = new JOrbisAudioStream(in)) {
            if (stream.readAll().remaining() == 0) {
                throw new AssertionError("Decoded audio is empty");
            }
        }
    }

    private static void writePack(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        byte[] audio;
        try (InputStream in = SpeakClientGameTest.class.getResourceAsStream("/audio/three_seconds.ogg")) {
            audio = in.readAllBytes();
        }
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("voicepack.json"));
            zip.write(MANIFEST.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(LINE));
            zip.write(audio);
            zip.closeEntry();
        }
    }
}

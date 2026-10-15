package me.teajay.talkingvillagers.test;

import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
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
 * A player fights a husk next to a villager: the villager cheers while the player hits it and reacts to the kill.
 */
public class CheerClientGameTest implements FabricClientGameTest {
    private static final String CHEER = "sounds/cheerer/cheer/go.ogg";
    private static final String KILLED = "sounds/cheerer/monster_killed/well_done.ogg";
    private static final String MANIFEST = """
            {
              "format": 2, "id": "cheertest", "name": "Cheer Test", "version": "1.0.0", "language": "en",
              "voices": [{
                "id": "cheerer", "name": "Cheerer", "entities": ["villager"], "gender": "male",
                "lines": {
                  "hurt": [{ "file": "%1$s" }], "death": [{ "file": "%1$s" }],
                  "cheer": [{ "file": "%1$s", "text": "Go, get him!" }],
                  "monster_killed": [{ "file": "%2$s", "text": "Well done!" }]
                }
              }]
            }
            """.formatted(CHEER, KILLED);

    @Override
    public void runTest(ClientGameTestContext context) {
        Path pack = Voicepacks.repository().directory().resolve("cheertest.voicepack");
        try {
            writePack(pack);
            context.runOnClient(client -> Voicepacks.reload());
            try (TestSingleplayerContext world = context.worldBuilder().create()) {
                var server = world.getServer();
                server.runCommand("time set day");
                server.runCommand("summon minecraft:villager ~3 ~ ~ {NoAI:1b,Tags:[tvcheer]}");
                server.runCommand("summon minecraft:husk ~3 ~ ~2 {NoAI:1b,Tags:[tvhusk],Health:200f,Attributes:[{id:'minecraft:max_health',base:200d}]}");
                context.waitTicks(20);
                server.runCommand("tv voice set @e[tag=tvcheer] cheertest:cheerer force");

                // the cheer has a chance, so hit a few times; the same player is cheered at most every 5 seconds
                boolean cheered = false;
                for (int attempt = 0; attempt < 6 && !cheered; attempt++) {
                    server.runCommand("damage @e[tag=tvhusk,limit=1] 1 minecraft:player_attack by @p");
                    context.waitTicks(110);
                    cheered = server.computeOnServer(server1 -> said(server1, "cheer"));
                }
                if (!cheered) {
                    throw new AssertionError("The villager did not cheer");
                }

                server.runCommand("damage @e[tag=tvhusk,limit=1] 1000 minecraft:player_attack by @p");
                context.waitTicks(10);
                if (!server.computeOnServer(server1 -> said(server1, "monster_killed"))) {
                    throw new AssertionError("The villager did not react to the kill");
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

    private static boolean said(MinecraftServer server, String event) {
        for (Entity entity : server.overworld().getAllEntities()) {
            if (entity instanceof Villager villager && villager.entityTags().contains("tvcheer")) {
                return EntityVoices.state(villager).lastLine(event).isPresent();
            }
        }
        return false;
    }

    private static void writePack(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        byte[] audio;
        try (InputStream in = CheerClientGameTest.class.getResourceAsStream("/audio/three_seconds.ogg")) {
            audio = in.readAllBytes();
        }
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("voicepack.json"));
            zip.write(MANIFEST.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (String line : new String[]{CHEER, KILLED}) {
                zip.putNextEntry(new ZipEntry(line));
                zip.write(audio);
                zip.closeEntry();
            }
        }
    }
}

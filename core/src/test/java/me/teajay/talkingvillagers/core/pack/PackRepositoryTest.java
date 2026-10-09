package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.TestPacks;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static me.teajay.talkingvillagers.core.TestPacks.BASIC_FILES;
import static me.teajay.talkingvillagers.core.TestPacks.BASIC_LINES;
import static me.teajay.talkingvillagers.core.TestPacks.manifest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackRepositoryTest {
    @TempDir
    Path dir;

    private static TestPacks pack(String id, String version) {
        return TestPacks.create()
                .manifest(manifest("", BASIC_LINES).replace("\"id\": \"test\"", "\"id\": \"" + id + "\"").replace("\"1.0.0\"", "\"" + version + "\""))
                .sounds(BASIC_FILES);
    }

    @Test
    void createsFolderAndStartsEmpty() throws Exception {
        PackRepository repository = new PackRepository(dir.resolve("voicepacks"));

        PackIndex index = repository.reload();

        assertThat(Files.isDirectory(dir.resolve("voicepacks"))).isTrue();
        assertThat(index.all()).isEmpty();
        assertThat(index.voicesFor(EntityKind.VILLAGER)).isEmpty();
    }

    @Test
    void loadsPacksAndIndexesVoices() throws Exception {
        pack("one", "1.0.0").write(dir, "one.voicepack");
        TestPacks.create().manifest("{}").write(dir, "broken.voicepack");
        Files.writeString(dir.resolve("notes.txt"), "ignored");
        PackRepository repository = new PackRepository(dir);

        PackIndex index = repository.reload();

        assertThat(index.all()).hasSize(2);
        assertThat(index.active()).containsOnlyKeys("one");
        assertThat(index.voice(new VoiceRef("one", "anna"))).isPresent();
        assertThat(index.voicesFor(EntityKind.VILLAGER)).hasSize(1);
        assertThat(index.voicesFor(EntityKind.WITCH)).isEmpty();
        assertThat(index.languages()).containsExactly("de");
        assertThat(repository.index()).isSameAs(index);
    }

    @Test
    void newestVersionWinsForSamePackId() throws Exception {
        pack("same", "1.9.0").write(dir, "a.voicepack");
        pack("same", "1.10.0").write(dir, "b.voicepack");

        PackIndex index = new PackRepository(dir).reload();

        LoadedPack active = index.active().get("same");
        assertThat(active.pack().orElseThrow().version()).isEqualTo("1.10.0");
        // the older file stays reachable by hash, e.g. for a server that still uses it
        assertThat(index.byHash()).hasSize(2);
    }

    @Test
    void disabledPacksAreNotActive() throws Exception {
        pack("one", "1.0.0").write(dir, "one.voicepack");
        pack("two", "1.0.0").write(dir, "two.voicepack");
        PackRepository repository = new PackRepository(dir);
        repository.setEnabled(id -> !id.equals("two"));

        PackIndex index = repository.reload();

        assertThat(index.active()).containsOnlyKeys("one");
    }

    @Test
    void readsFilesByHash() throws Exception {
        pack("one", "1.0.0").write(dir, "one.voicepack");
        PackRepository repository = new PackRepository(dir);
        String hash = repository.reload().active().get("one").sha256();

        assertThat(repository.readFile(hash, BASIC_FILES[0])).isEqualTo(TestPacks.audio("mono.ogg"));
        assertThatThrownBy(() -> repository.readFile(hash, "sounds/missing.ogg")).hasMessageContaining("No file");
        assertThatThrownBy(() -> repository.readFile("0".repeat(64), BASIC_FILES[0])).hasMessageContaining("No voicepack");
    }
}

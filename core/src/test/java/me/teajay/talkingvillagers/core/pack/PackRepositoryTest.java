package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.TestPacks;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
    void cachedPacksArePlayableButNeverActive() throws Exception {
        PackRepository repository = new PackRepository(dir);
        Files.createDirectories(repository.cacheDirectory());
        Path cached = pack("server", "1.0.0").write(repository.cacheDirectory(), "abc.voicepack");

        PackIndex index = repository.reload();

        assertThat(index.active()).isEmpty();
        assertThat(index.cached()).hasSize(1);
        String hash = index.cached().getFirst().sha256();
        assertThat(index.byHash(hash)).isPresent();
        assertThat(repository.readFile(hash, BASIC_FILES[0])).isNotEmpty();

        Path installed = repository.installCached(index.cached().getFirst());
        assertThat(installed.getFileName().toString()).isEqualTo("server-1.0.0.voicepack");
        assertThat(repository.reload().active()).containsOnlyKeys("server");
        assertThat(Files.exists(cached)).isTrue();
    }

    @Test
    void installCachedNeverWritesOutsideTheFolder() throws Exception {
        PackRepository repository = new PackRepository(dir.resolve("voicepacks"));
        Files.createDirectories(repository.cacheDirectory());
        Path cached = pack("server", "1.0.0").write(repository.cacheDirectory(), "abc.voicepack");
        VoicePack valid = repository.reload().cached().getFirst().pack().orElseThrow();
        // a pack that got past validation must still not escape, e.g. if the patterns are ever loosened
        VoicePack hostile = new VoicePack(valid.format(), valid.id(), valid.name(), "x/../../../evil", valid.description(),
                valid.authors(), valid.language(), valid.voices());

        assertThatThrownBy(() -> repository.installCached(new LoadedPack(cached, "abc", Optional.of(hostile), List.of(), false)))
                .isInstanceOf(IOException.class);
        try (Stream<Path> files = Files.walk(dir)) {
            assertThat(files.filter(file -> file.getFileName().toString().startsWith("evil"))).isEmpty();
        }
    }

    @Test
    void pruneCacheDeletesOldestFilesFirst() throws Exception {
        PackRepository repository = new PackRepository(dir);
        Files.createDirectories(repository.cacheDirectory());
        Path old = Files.write(repository.cacheDirectory().resolve("old.voicepack"), new byte[1000]);
        Path young = Files.write(repository.cacheDirectory().resolve("young.voicepack"), new byte[1000]);
        Files.setLastModifiedTime(old, FileTime.fromMillis(1000));

        repository.pruneCache(1500, Set.of());

        assertThat(Files.exists(old)).isFalse();
        assertThat(Files.exists(young)).isTrue();
    }

    @Test
    void pruneCacheKeepsPacksInUseAndUnfinishedDownloads() throws Exception {
        PackRepository repository = new PackRepository(dir);
        Files.createDirectories(repository.cacheDirectory());
        String current = "a".repeat(64);
        Path inUse = Files.write(repository.cacheDirectory().resolve(current + ".voicepack"), new byte[1000]);
        Path part = Files.write(repository.cacheDirectory().resolve("b".repeat(64) + ".part"), new byte[1000]);
        Path other = Files.write(repository.cacheDirectory().resolve("c".repeat(64) + ".voicepack"), new byte[1000]);
        Files.setLastModifiedTime(inUse, FileTime.fromMillis(1000));
        Files.setLastModifiedTime(part, FileTime.fromMillis(1000));

        repository.pruneCache(500, Set.of(current));

        assertThat(inUse).exists();
        assertThat(part).exists();
        assertThat(other).doesNotExist();
    }

    @Test
    void markingCachedPacksAsUsedProtectsThemFromPruning() throws Exception {
        PackRepository repository = new PackRepository(dir);
        Files.createDirectories(repository.cacheDirectory());
        String used = "a".repeat(64);
        Path usedFile = Files.write(repository.cacheDirectory().resolve(used + ".voicepack"), new byte[1000]);
        Path other = Files.write(repository.cacheDirectory().resolve("c".repeat(64) + ".voicepack"), new byte[1000]);
        Files.setLastModifiedTime(usedFile, FileTime.fromMillis(1000));
        Files.setLastModifiedTime(other, FileTime.fromMillis(2000));

        repository.markCachedUsed(Set.of(used, "../escape"));
        repository.pruneCache(1500, Set.of());

        assertThat(usedFile).exists();
        assertThat(other).doesNotExist();
    }

    @Test
    void deletesUnfinishedDownloads() throws Exception {
        PackRepository repository = new PackRepository(dir);
        Files.createDirectories(repository.cacheDirectory());
        Path part = Files.write(repository.cacheDirectory().resolve("a".repeat(64) + ".part"), new byte[10]);
        Path pack = Files.write(repository.cacheDirectory().resolve("b".repeat(64) + ".voicepack"), new byte[10]);

        repository.deletePartFiles();

        assertThat(part).doesNotExist();
        assertThat(pack).exists();
    }

    @Test
    void installingACachedPackKeepsOtherFilesWithTheSameName() throws Exception {
        Path cache = dir.resolve(PackRepository.CACHE_DIRECTORY);
        Files.createDirectories(cache);
        pack("one", "1.0.0").write(cache, "a".repeat(64) + ".voicepack");
        Path foreign = Files.writeString(dir.resolve("one-1.0.0.voicepack"), "not a pack");
        PackRepository repository = new PackRepository(dir);
        LoadedPack cached = repository.reload().cached().getFirst();

        Path installed = repository.installCached(cached);

        assertThat(installed.getFileName()).hasToString("one-1.0.0-2.voicepack");
        assertThat(foreign).hasContent("not a pack");
        assertThat(repository.reload().active()).containsKey("one");
    }

    @Test
    void readsFilesByHash() throws Exception {
        pack("one", "1.0.0").write(dir, "one.voicepack");
        PackRepository repository = new PackRepository(dir);
        String hash = repository.reload().active().get("one").sha256();

        assertThat(repository.readFile(hash, BASIC_FILES[0])).isEqualTo(TestPacks.audio("mono.ogg"));
        assertThatThrownBy(() -> repository.readFile(hash, "sounds/missing.ogg")).hasMessageContaining("No file");
        assertThatThrownBy(() -> repository.readFile("0".repeat(64), BASIC_FILES[0])).hasMessageContaining("No voicepack");
        assertThatThrownBy(() -> repository.readFile(hash, BASIC_FILES[0], 10)).hasMessageContaining("larger than");
    }
}

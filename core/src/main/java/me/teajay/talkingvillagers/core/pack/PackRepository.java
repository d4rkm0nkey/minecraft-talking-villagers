package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.io.ZipPackContents;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The {@code .voicepack} files in a folder. Reloading builds a new {@link PackIndex} that replaces the old one atomically,
 * so readers on other threads always see a consistent snapshot.
 */
public final class PackRepository {
    private final Path directory;
    private volatile Predicate<String> enabled = id -> true;
    private volatile PackIndex index = PackIndex.EMPTY;

    public PackRepository(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return directory;
    }

    public PackIndex index() {
        return index;
    }

    public void setEnabled(Predicate<String> enabled) {
        this.enabled = enabled;
    }

    /**
     * Reads all packs in the folder, creating the folder if needed.
     */
    public synchronized PackIndex reload() throws IOException {
        Files.createDirectories(directory);
        List<LoadedPack> packs = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.sorted().toList()) {
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(VoicePack.FILE_EXTENSION)) {
                    packs.add(PackReader.read(file));
                }
            }
        }
        PackIndex newIndex = PackIndex.build(packs, enabled);
        this.index = newIndex;
        return newIndex;
    }

    /**
     * Reads a file from a pack. The archive is opened for each read so pack files are never kept locked.
     */
    public byte[] readFile(String sha256, String path) throws IOException {
        LoadedPack pack = index.byHash(sha256).orElseThrow(() -> new IOException("No voicepack with hash " + sha256));
        try (ZipPackContents contents = new ZipPackContents(pack.source()); InputStream in = contents.open(path)) {
            return in.readAllBytes();
        }
    }
}

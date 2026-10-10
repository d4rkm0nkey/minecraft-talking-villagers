package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.io.ZipPackContents;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The {@code .voicepack} files in a folder. Reloading builds a new {@link PackIndex} that replaces the old one atomically,
 * so readers on other threads always see a consistent snapshot.
 */
public final class PackRepository {
    /** Packs downloaded from servers live here, named by their SHA-256. */
    public static final String CACHE_DIRECTORY = ".server-cache";
    private final Path directory;
    private volatile Predicate<String> enabled = id -> true;
    private volatile PackIndex index = PackIndex.EMPTY;

    public PackRepository(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return directory;
    }

    public Path cacheDirectory() {
        return directory.resolve(CACHE_DIRECTORY);
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
        List<LoadedPack> packs = readAll(directory);
        List<LoadedPack> cached = Files.isDirectory(cacheDirectory()) ? readAll(cacheDirectory()) : List.of();
        PackIndex newIndex = PackIndex.build(packs, cached, enabled);
        this.index = newIndex;
        return newIndex;
    }

    private static List<LoadedPack> readAll(Path folder) throws IOException {
        List<LoadedPack> packs = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.sorted().toList()) {
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(VoicePack.FILE_EXTENSION)) {
                    packs.add(PackReader.read(file));
                }
            }
        }
        return packs;
    }

    /**
     * Copies a pack downloaded from a server into the voicepacks folder, so it is also used in other worlds.
     *
     * @return the installed file
     */
    public Path installCached(LoadedPack cached) throws IOException {
        VoicePack pack = cached.pack().orElseThrow(() -> new IOException("Broken pack " + cached.source()));
        Path target = directory.resolve(pack.id() + "-" + pack.version() + VoicePack.FILE_EXTENSION);
        Files.copy(cached.source(), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /**
     * Deletes the oldest downloaded packs until the cache is at most {@code maxBytes} large.
     */
    public void pruneCache(long maxBytes) throws IOException {
        if (!Files.isDirectory(cacheDirectory())) {
            return;
        }
        List<Path> files;
        try (Stream<Path> stream = Files.list(cacheDirectory())) {
            files = new ArrayList<>(stream.filter(Files::isRegularFile).toList());
        }
        files.sort(Comparator.comparing(file -> {
            try {
                return Files.getLastModifiedTime(file).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }));
        long total = 0;
        for (Path file : files) {
            total += Files.size(file);
        }
        for (Path file : files) {
            if (total <= maxBytes) {
                break;
            }
            total -= Files.size(file);
            Files.deleteIfExists(file);
        }
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

package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.io.ZipPackContents;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.util.SafeFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The {@code .voicepack} files in a folder. Reloading builds a new {@link PackIndex} that replaces the old one atomically,
 * so readers on other threads always see a consistent snapshot.
 */
public final class PackRepository {
    /** Packs downloaded from servers live here, named by their SHA-256. */
    public static final String CACHE_DIRECTORY = ".server-cache";
    /** Unfinished downloads in the cache. */
    public static final String PART_EXTENSION = ".part";
    /** Largest file {@link #readFile} reads from a pack; lines are at most 60 seconds long. */
    public static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
    private final Path directory;
    private volatile Predicate<String> enabled = id -> true;
    private volatile PackIndex index = PackIndex.EMPTY;
    /** Packs read by earlier reloads, so unchanged files are neither read nor hashed again. Guarded by {@code this}. */
    private final Map<Path, CachedRead> reads = new HashMap<>();

    private record CachedRead(long size, long modified, LoadedPack pack) {
    }

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
     * Reads all packs in the folder, creating the folder if needed. Files with the same size and modification time as
     * at the last reload are taken from memory.
     */
    public synchronized PackIndex reload() throws IOException {
        Files.createDirectories(directory);
        Map<Path, CachedRead> seen = new HashMap<>();
        List<LoadedPack> packs = readAll(directory, seen);
        List<LoadedPack> cached = Files.isDirectory(cacheDirectory()) ? readAll(cacheDirectory(), seen) : List.of();
        reads.clear();
        reads.putAll(seen);
        PackIndex newIndex = PackIndex.build(packs, cached, enabled);
        this.index = newIndex;
        return newIndex;
    }

    private List<LoadedPack> readAll(Path folder, Map<Path, CachedRead> seen) throws IOException {
        List<LoadedPack> packs = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.sorted().toList()) {
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(VoicePack.FILE_EXTENSION)) {
                    CachedRead read = read(file);
                    seen.put(file, read);
                    packs.add(read.pack());
                }
            }
        }
        return packs;
    }

    private CachedRead read(Path file) {
        long size;
        long modified;
        try {
            size = Files.size(file);
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            // deleted in the meantime; the reader reports it as broken
            return new CachedRead(-1, -1, PackReader.read(file));
        }
        CachedRead earlier = reads.get(file);
        if (earlier != null && earlier.size() == size && earlier.modified() == modified) {
            return earlier;
        }
        return new CachedRead(size, modified, PackReader.read(file));
    }

    /**
     * Copies a pack downloaded from a server into the voicepacks folder, so it is also used in other worlds.
     * Another file with the same name is kept, the copy then gets a number.
     *
     * @return the installed file
     */
    public Path installCached(LoadedPack cached) throws IOException {
        VoicePack pack = cached.pack().orElseThrow(() -> new IOException("Broken pack " + cached.source()));
        // id and version come from a server; the manifest patterns rule out separators, this check keeps it that way
        Path folder = directory.toAbsolutePath().normalize();
        Path target = folder.resolve(pack.id() + "-" + pack.version() + VoicePack.FILE_EXTENSION).normalize();
        if (!folder.equals(target.getParent())) {
            throw new IOException("Invalid file name for pack " + pack.id() + " " + pack.version());
        }
        target = SafeFiles.freeName(target);
        SafeFiles.copyAtomically(cached.source(), target);
        return target;
    }

    /**
     * Marks downloaded packs as used now, so {@link #pruneCache} deletes them last.
     */
    public void markCachedUsed(Set<String> sha256s) {
        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        for (String sha256 : sha256s) {
            if (!LoadedPack.isSha256(sha256)) {
                continue;
            }
            Path file = cacheDirectory().resolve(sha256 + VoicePack.FILE_EXTENSION);
            try {
                if (Files.isRegularFile(file)) {
                    Files.setLastModifiedTime(file, now);
                }
            } catch (IOException ignored) {
                // only affects the order of pruning
            }
        }
    }

    /**
     * Deletes the least recently used downloaded packs until the cache is at most {@code maxBytes} large.
     * Unfinished downloads and the packs in {@code keep} (by SHA-256, e.g. those of the current server) stay.
     */
    public void pruneCache(long maxBytes, Set<String> keep) throws IOException {
        if (!Files.isDirectory(cacheDirectory())) {
            return;
        }
        record CachedFile(Path path, long size, long modified) {
        }
        List<CachedFile> files = new ArrayList<>();
        long total = 0;
        try (Stream<Path> stream = Files.list(cacheDirectory())) {
            for (Path file : stream.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(VoicePack.FILE_EXTENSION) || !Files.isRegularFile(file)) {
                    continue;
                }
                try {
                    CachedFile cached = new CachedFile(file, Files.size(file), Files.getLastModifiedTime(file).toMillis());
                    total += cached.size();
                    if (!keep.contains(name.substring(0, name.length() - VoicePack.FILE_EXTENSION.length()))) {
                        files.add(cached);
                    }
                } catch (IOException ignored) {
                    // deleted in the meantime
                }
            }
        }
        files.sort(Comparator.comparingLong(CachedFile::modified));
        for (CachedFile file : files) {
            if (total <= maxBytes) {
                break;
            }
            try {
                Files.deleteIfExists(file.path());
                total -= file.size();
            } catch (IOException ignored) {
                // e.g. locked on Windows while a sound is read; try again next time
            }
        }
    }

    /**
     * Deletes unfinished downloads, e.g. after the game was closed during a download.
     */
    public void deletePartFiles() throws IOException {
        if (!Files.isDirectory(cacheDirectory())) {
            return;
        }
        try (Stream<Path> stream = Files.list(cacheDirectory())) {
            for (Path file : stream.filter(file -> file.getFileName().toString().endsWith(PART_EXTENSION)).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    /**
     * Reads a file of at most {@value #MAX_FILE_BYTES} bytes from a pack.
     */
    public byte[] readFile(String sha256, String path) throws IOException {
        return readFile(sha256, path, MAX_FILE_BYTES);
    }

    /**
     * Reads a file from a pack. The archive is opened for each read so pack files are never kept locked.
     */
    public byte[] readFile(String sha256, String path, int maxBytes) throws IOException {
        LoadedPack pack = index.byHash(sha256).orElseThrow(() -> new IOException("No voicepack with hash " + sha256));
        try (ZipPackContents contents = new ZipPackContents(pack.source()); InputStream in = contents.open(path)) {
            return SafeFiles.readAtMost(in, maxBytes, path);
        }
    }
}

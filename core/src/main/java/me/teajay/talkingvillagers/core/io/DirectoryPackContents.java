package me.teajay.talkingvillagers.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The files of an unpacked pack in a directory, with {@code /} separated paths relative to the directory.
 */
public final class DirectoryPackContents implements PackContents {
    private final Path root;
    private final Set<String> files;

    public DirectoryPackContents(Path root) throws IOException {
        this.root = root;
        Set<String> names = new LinkedHashSet<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).sorted()
                    .forEach(path -> names.add(root.relativize(path).toString().replace('\\', '/')));
        }
        this.files = Collections.unmodifiableSet(names);
    }

    @Override
    public Set<String> files() {
        return files;
    }

    @Override
    public long size(String path) {
        try {
            return exists(path) ? Files.size(root.resolve(path)) : -1;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream open(String path) throws IOException {
        if (!exists(path)) {
            throw new IOException("No file " + path + " in " + root);
        }
        return Files.newInputStream(root.resolve(path));
    }
}

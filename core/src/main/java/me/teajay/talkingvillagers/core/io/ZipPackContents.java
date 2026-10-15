package me.teajay.talkingvillagers.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads files directly from a {@code .voicepack} ZIP without extracting it.
 */
public final class ZipPackContents implements PackContents, AutoCloseable {
    private final ZipFile zip;
    private final Set<String> files;

    public ZipPackContents(Path file) throws IOException {
        this.zip = new ZipFile(file.toFile());
        Set<String> names = new LinkedHashSet<>();
        zip.stream().filter(entry -> !entry.isDirectory()).forEach(entry -> names.add(entry.getName()));
        this.files = Collections.unmodifiableSet(names);
    }

    @Override
    public Set<String> files() {
        return files;
    }

    @Override
    public long size(String path) {
        ZipEntry entry = zip.getEntry(path);
        return entry == null ? -1 : entry.getSize();
    }

    @Override
    public InputStream open(String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null || entry.isDirectory()) {
            throw new IOException("No file " + path + " in " + zip.getName());
        }
        return zip.getInputStream(entry);
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}

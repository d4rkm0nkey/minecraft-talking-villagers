package me.teajay.talkingvillagers.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File helpers that never leave a half written pack behind and never read more than they should.
 */
public final class SafeFiles {
    private static final String TEMP_SUFFIX = ".tmp";

    private SafeFiles() {
    }

    /** Writes a file. */
    @FunctionalInterface
    public interface Writer {
        void write(Path file) throws IOException;
    }

    /**
     * Lets {@code writer} fill a temporary file next to {@code target} and moves it into place once it is complete.
     */
    public static void writeAtomically(Path target, Writer writer) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
        try {
            writer.write(temp);
            move(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static void copyAtomically(Path source, Path target) throws IOException {
        writeAtomically(target, temp -> Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING));
    }

    /**
     * Replaces {@code target} with {@code source} in one step where the file system allows it.
     */
    public static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * {@code file} if it does not exist yet, otherwise the first free name with a number, e.g. {@code pack-2.voicepack}.
     */
    public static Path freeName(Path file) {
        if (!Files.exists(file)) {
            return file;
        }
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; ; i++) {
            Path candidate = file.resolveSibling(stem + "-" + i + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * Reads the whole stream, but fails instead of reading more than {@code maxBytes}.
     */
    public static byte[] readAtMost(InputStream in, int maxBytes, String name) throws IOException {
        byte[] data = in.readNBytes(maxBytes + 1);
        if (data.length > maxBytes) {
            throw new IOException("'" + name + "' is larger than " + maxBytes / 1024 + " KiB");
        }
        return data;
    }
}

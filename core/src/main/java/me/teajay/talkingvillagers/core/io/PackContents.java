package me.teajay.talkingvillagers.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Files inside a voicepack archive, addressed by their path relative to the archive root.
 */
public interface PackContents {
    Set<String> files();

    default boolean exists(String path) {
        return files().contains(path);
    }

    /**
     * Uncompressed size in bytes, or -1 if unknown.
     */
    long size(String path);

    InputStream open(String path) throws IOException;
}

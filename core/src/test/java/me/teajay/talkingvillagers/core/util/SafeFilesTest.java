package me.teajay.talkingvillagers.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeFilesTest {
    @TempDir
    Path dir;

    @Test
    void failedWriteKeepsTheOldFile() throws Exception {
        Path target = Files.writeString(dir.resolve("pack.voicepack"), "old");

        assertThatThrownBy(() -> SafeFiles.writeAtomically(target, temp -> {
            Files.writeString(temp, "half");
            throw new IOException("disk full");
        })).hasMessage("disk full");

        assertThat(target).hasContent("old");
        try (var files = Files.list(dir)) {
            assertThat(files).containsExactly(target);
        }
    }

    @Test
    void writeReplacesTheFileWhenComplete() throws Exception {
        Path target = Files.writeString(dir.resolve("pack.voicepack"), "old");

        SafeFiles.writeAtomically(target, temp -> Files.writeString(temp, "new"));

        assertThat(target).hasContent("new");
    }

    @Test
    void freeNameAddsANumber() throws Exception {
        Path file = dir.resolve("pack.voicepack");
        assertThat(SafeFiles.freeName(file)).isEqualTo(file);

        Files.createFile(file);
        Files.createFile(dir.resolve("pack-2.voicepack"));

        assertThat(SafeFiles.freeName(file)).isEqualTo(dir.resolve("pack-3.voicepack"));
    }

    @Test
    void readAtMostRefusesLargerStreams() throws Exception {
        assertThat(SafeFiles.readAtMost(new ByteArrayInputStream(new byte[10]), 10, "a")).hasSize(10);
        assertThatThrownBy(() -> SafeFiles.readAtMost(new ByteArrayInputStream(new byte[11]), 10, "a"))
                .hasMessageContaining("'a' is larger than");
    }
}

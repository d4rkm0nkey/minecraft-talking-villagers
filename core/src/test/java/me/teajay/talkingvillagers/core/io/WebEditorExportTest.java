package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.model.VoicePack;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A pack exported by the web editor (its export test writes it) must load in the mod without problems.
 * Skipped if the web editor tests have not run.
 */
class WebEditorExportTest {
    @Test
    void webEditorExportLoadsWithoutProblems() {
        Path file = Path.of(System.getProperty("talkingvillagers.webExport"));
        assumeTrue(Files.exists(file), "Run the web editor tests (npm test) first");

        LoadedPack loaded = PackReader.read(file);

        assertThat(loaded.problems()).isEmpty();
        VoicePack pack = loaded.pack().orElseThrow();
        assertThat(pack.id()).isEqualTo("export-test");
        var greeting = pack.voice("anna").orElseThrow().lines(EventCatalog.GREETING).getFirst();
        assertThat(greeting.text()).contains("Hallo!");
        assertThat(greeting.duration()).hasValueSatisfying(duration -> assertThat(duration).isBetween(0.6f, 0.9f));
    }
}

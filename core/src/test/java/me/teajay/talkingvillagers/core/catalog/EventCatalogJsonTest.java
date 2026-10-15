package me.teajay.talkingvillagers.core.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EventCatalogJsonTest {
    @Test
    void eventsJsonIsUpToDate() throws Exception {
        Path file = Path.of(System.getProperty("talkingvillagers.schemaDir"), "events.json");

        assertThat(Files.readString(file))
                .as("schema/events.json is outdated, run ./gradlew :core:writeEventCatalog")
                .isEqualTo(EventCatalogJson.render());
    }
}

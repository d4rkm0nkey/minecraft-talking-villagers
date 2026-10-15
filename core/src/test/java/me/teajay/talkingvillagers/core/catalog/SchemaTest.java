package me.teajay.talkingvillagers.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import me.teajay.talkingvillagers.core.TestPacks;
import me.teajay.talkingvillagers.core.io.ManifestCodec;
import me.teajay.talkingvillagers.core.model.VoicePack;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the shared JSON schema (used by the web editor) and the Java parser in sync.
 */
class SchemaTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SCHEMA_DIR = Path.of(System.getProperty("talkingvillagers.schemaDir"));

    private static JsonNode schemaJson() throws Exception {
        return MAPPER.readTree(SCHEMA_DIR.resolve("voicepack.schema.json").toFile());
    }

    private static JsonSchema schema() throws Exception {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson());
    }

    private static Set<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return Set.copyOf(values);
    }

    @Test
    void eventsMatchCatalog() throws Exception {
        Set<String> catalog = EventCatalog.all().stream().map(VoiceEvent::id).collect(Collectors.toSet());

        assertThat(strings(schemaJson().at("/$defs/event/enum"))).isEqualTo(catalog);
    }

    @Test
    void entitiesMatchEntityKinds() throws Exception {
        Set<String> kinds = new java.util.HashSet<>(Set.of(EntityKind.ILLAGER_GROUP));
        for (EntityKind kind : EntityKind.values()) {
            kinds.add(kind.id());
        }

        assertThat(strings(schemaJson().at("/$defs/entity/enum"))).isEqualTo(kinds);
    }

    @Test
    void professionsMatchVanillaList() throws Exception {
        assertThat(strings(schemaJson().at("/$defs/profession/anyOf/1/enum"))).isEqualTo(Professions.VANILLA);
    }

    @Test
    void formatVersionMatches() throws Exception {
        assertThat(schemaJson().at("/properties/format/const").asInt()).isEqualTo(VoicePack.FORMAT);
    }

    @Test
    void exampleIsValidForSchemaAndParser() throws Exception {
        String example = Files.readString(SCHEMA_DIR.resolve("examples/voicepack.json"));

        Set<ValidationMessage> messages = schema().validate(MAPPER.readTree(example));

        assertThat(messages).isEmpty();
        assertThat(ManifestCodec.parse(example).problems()).isEmpty();
    }

    @Test
    void writtenManifestIsValidForSchema() throws Exception {
        VoicePack pack = ManifestCodec.parse(Files.readString(SCHEMA_DIR.resolve("examples/voicepack.json"))).pack().orElseThrow();

        assertThat(schema().validate(MAPPER.readTree(ManifestCodec.write(pack)))).isEmpty();
    }

    @Test
    void schemaAndParserRejectTheSameManifests() throws Exception {
        String valid = TestPacks.manifest("", TestPacks.BASIC_LINES);
        List<String> invalid = List.of(
                valid.replace("\"format\": 2", "\"format\": 1"),
                valid.replace("\"id\": \"test\"", "\"id\": \"Test\""),
                valid.replace("\"language\": \"de\"", "\"language\": \"de_DE\""),
                valid.replace("\"gender\": \"female\"", "\"gender\": \"other\""),
                valid.replace("[\"villager\"]", "[\"zombie\"]"),
                valid.replace("[\"villager\"]", "[]"),
                valid.replace("sounds/anna/hurt/au.ogg", "sounds/../au.ogg"),
                valid.replace("sounds/anna/hurt/au.ogg", "sounds/anna/Hurt.ogg"),
                valid.replace("\"gender\": \"female\",", "\"gender\": \"female\", \"pitch\": { \"min\": 0.1, \"max\": 1.0 },"),
                valid.replace("\"gender\": \"female\",", "\"gender\": \"female\", \"weight\": 0,"),
                valid.replace("\"name\": \"Test\",", ""));

        assertThat(schema().validate(MAPPER.readTree(valid))).isEmpty();
        for (String json : invalid) {
            assertThat(schema().validate(MAPPER.readTree(json))).as("schema: %s", json).isNotEmpty();
            assertThat(ManifestCodec.parse(json).pack()).as("parser: %s", json).isEmpty();
        }
    }
}

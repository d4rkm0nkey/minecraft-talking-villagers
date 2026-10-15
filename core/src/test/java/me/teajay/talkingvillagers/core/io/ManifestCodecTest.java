package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.Weather;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static me.teajay.talkingvillagers.core.TestPacks.BASIC_LINES;
import static me.teajay.talkingvillagers.core.TestPacks.manifest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ManifestCodecTest {
    static String example() throws Exception {
        return Files.readString(Path.of(System.getProperty("talkingvillagers.schemaDir"), "examples", "voicepack.json"));
    }

    @Test
    void parsesExample() throws Exception {
        ManifestCodec.Result result = ManifestCodec.parse(example());

        assertThat(result.problems()).isEmpty();
        VoicePack pack = result.pack().orElseThrow();
        assertThat(pack.id()).isEqualTo("example-pack");
        assertThat(pack.language()).isEqualTo("de");
        assertThat(pack.authors()).containsExactly("TeaJay");

        Voice anna = pack.voice("anna").orElseThrow();
        assertThat(anna.entities()).containsExactlyInAnyOrder(EntityKind.VILLAGER, EntityKind.WANDERING_TRADER);
        assertThat(anna.gender()).isEqualTo(Gender.FEMALE);
        assertThat(anna.child()).isFalse();
        assertThat(anna.pitch()).isEqualTo(new PitchRange(0.9f, 1.1f));
        assertThat(anna.weight()).isEqualTo(2);
        VoiceLine farmerLine = anna.lines(EventCatalog.GREETING).get(1);
        assertThat(farmerLine.text()).contains("Schön, dass du die Felder besuchst.");
        assertThat(farmerLine.conditions().professions()).containsExactly("farmer");
        assertThat(farmerLine.conditions().weather()).contains(Weather.CLEAR);
        assertThat(anna.lines(EventCatalog.HURT).getFirst().duration()).contains(0.5f);

        assertThat(pack.voice("paul-kid").orElseThrow().child()).isTrue();
        Voice grimm = pack.voice("grimm").orElseThrow();
        assertThat(grimm.entities()).containsExactlyInAnyOrderElementsOf(EntityKind.ILLAGERS);
        assertThat(grimm.language()).contains("en");
        assertThat(grimm.pitch()).isEqualTo(PitchRange.DEFAULT);
    }

    @Test
    void writtenManifestParsesToTheSamePack() throws Exception {
        VoicePack pack = ManifestCodec.parse(example()).pack().orElseThrow();

        ManifestCodec.Result reparsed = ManifestCodec.parse(ManifestCodec.write(pack));

        assertThat(reparsed.problems()).isEmpty();
        assertThat(reparsed.pack()).contains(pack);
    }

    @Test
    void reportsMissingFieldsWithPaths() {
        ManifestCodec.Result result = ManifestCodec.parse("""
                { "format": 2, "id": "x", "voices": [ { "id": "a", "entities": ["villager"], "lines": {} } ] }
                """);

        assertThat(result.pack()).isEmpty();
        assertThat(result.problems())
                .filteredOn(problem -> problem.code() == ProblemCode.MISSING_FIELD)
                .extracting(Problem::path)
                .containsExactlyInAnyOrder("name", "version", "language", "voices[0].name", "voices[0].gender");
    }

    @Test
    void rejectsInvalidJson() {
        assertThat(ManifestCodec.parse("{ nope").problems())
                .extracting(Problem::code).containsExactly(ProblemCode.INVALID_JSON);
        assertThat(ManifestCodec.parse("[]").problems())
                .extracting(Problem::code).containsExactly(ProblemCode.INVALID_JSON);
    }

    @Test
    void rejectsOtherFormats() {
        ManifestCodec.Result result = ManifestCodec.parse(manifest("", BASIC_LINES).replace("\"format\": 2", "\"format\": 3"));

        assertThat(result.pack()).isEmpty();
        assertThat(result.problems()).singleElement().satisfies(problem -> {
            assertThat(problem.code()).isEqualTo(ProblemCode.UNSUPPORTED_FORMAT);
            assertThat(problem.message()).contains("Update Talking Villagers");
        });
    }

    @Test
    void rejectsInvalidValues() {
        String json = manifest("\"pitch\": { \"min\": 1.5, \"max\": 1.0 }, \"weight\": 0,", BASIC_LINES)
                .replace("\"id\": \"test\"", "\"id\": \"Test Pack\"")
                .replace("\"language\": \"de\"", "\"language\": \"deutsch!\"")
                .replace("\"gender\": \"female\"", "\"gender\": \"robot\"");

        ManifestCodec.Result result = ManifestCodec.parse(json);

        assertThat(result.pack()).isEmpty();
        assertThat(result.problems()).filteredOn(Problem::isError).extracting(Problem::path)
                .containsExactlyInAnyOrder("id", "language", "voices[0].gender", "voices[0].pitch", "voices[0].weight");
    }

    @Test
    void rejectsDuplicateVoiceIds() {
        String voice = """
                { "id": "anna", "name": "Anna", "entities": ["witch"], "gender": "female", "lines": {} }""";
        String json = manifest("", BASIC_LINES).replace("\"voices\": [", "\"voices\": [" + voice + ",");

        assertThat(ManifestCodec.parse(json).problems()).extracting(Problem::code).contains(ProblemCode.DUPLICATE_VOICE_ID);
    }

    @Test
    void rejectsUnknownEntities() {
        String json = manifest("", BASIC_LINES).replace("[\"villager\"]", "[\"villager\", \"creeper\"]");

        ManifestCodec.Result result = ManifestCodec.parse(json);

        assertThat(result.pack()).isEmpty();
        assertThat(result.problems()).extracting(Problem::code, Problem::path)
                .containsExactly(tuple(ProblemCode.UNKNOWN_ENTITY, "voices[0].entities[1]"));
    }

    @Test
    void rejectsFilePathsOutsideSounds() {
        for (String file : new String[]{"anna/hurt.ogg", "sounds/../voicepack.ogg", "sounds/Anna/hurt.ogg", "sounds/anna/hurt.wav", "/sounds/a.ogg"}) {
            ManifestCodec.Result result = ManifestCodec.parse(manifest("", "\"hurt\": [{ \"file\": \"" + file + "\" }]"));
            assertThat(result.problems()).as(file).extracting(Problem::code).containsExactly(ProblemCode.INVALID_FILE_PATH);
        }
    }

    @Test
    void ignoresUnknownEventsAndPropertiesWithWarning() {
        String json = manifest("\"mood\": \"happy\",", BASIC_LINES + ", \"sneeze\": [{ \"file\": \"sounds/anna/sneeze.ogg\" }]");

        ManifestCodec.Result result = ManifestCodec.parse(json);

        assertThat(result.pack()).isPresent();
        assertThat(result.pack().get().voices().getFirst().lines()).containsOnlyKeys("hurt", "death");
        assertThat(result.problems()).extracting(Problem::code, Problem::path).containsExactlyInAnyOrder(
                tuple(ProblemCode.UNKNOWN_PROPERTY, "voices[0].mood"),
                tuple(ProblemCode.UNKNOWN_EVENT, "voices[0].lines.sneeze"));
    }

    @Test
    void normalisesProfessionsAndWarnsAboutUnknownOnes() {
        String json = manifest("", BASIC_LINES + """
                , "random": [{ "file": "sounds/anna/random/a.ogg", "conditions": { "professions": ["minecraft:farmer", "baker", "mymod:baker"] } }]
                """);

        ManifestCodec.Result result = ManifestCodec.parse(json);

        assertThat(result.pack().orElseThrow().voices().getFirst().lines(EventCatalog.RANDOM).getFirst().conditions().professions())
                .containsExactlyInAnyOrder("farmer", "baker", "mymod:baker");
        assertThat(result.problems()).extracting(Problem::code, Problem::path).containsExactly(
                tuple(ProblemCode.UNKNOWN_PROFESSION, "voices[0].lines.random[0].conditions.professions[1]"));
    }

    @Test
    void normalisesLanguageTags() {
        ManifestCodec.Result result = ManifestCodec.parse(manifest("\"language\": \"en-gb\",", BASIC_LINES).replace("\"language\": \"de\"", "\"language\": \"DE\""));

        VoicePack pack = result.pack().orElseThrow();
        assertThat(pack.language()).isEqualTo("de");
        assertThat(pack.voices().getFirst().language()).contains("en-GB");
    }

    @Test
    void testManifestIsValid() {
        assertThat(ManifestCodec.parse(manifest("", BASIC_LINES)).problems()).isEmpty();
    }
}

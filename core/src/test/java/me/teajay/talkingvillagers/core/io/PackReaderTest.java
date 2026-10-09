package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.TestPacks;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static me.teajay.talkingvillagers.core.TestPacks.BASIC_FILES;
import static me.teajay.talkingvillagers.core.TestPacks.BASIC_LINES;
import static me.teajay.talkingvillagers.core.TestPacks.manifest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

class PackReaderTest {
    @TempDir
    Path dir;

    private LoadedPack read(TestPacks pack) throws Exception {
        return PackReader.read(pack.write(dir, "test.voicepack"));
    }

    @Test
    void loadsValidPackAndFillsInDurations() throws Exception {
        LoadedPack loaded = read(TestPacks.create()
                .manifest(manifest("", BASIC_LINES.replace("\"sounds/anna/death/oh.ogg\"", "\"sounds/anna/death/oh.ogg\", \"duration\": 2.5")))
                .sounds(BASIC_FILES)
                .file("icon.png", new byte[]{1}));

        assertThat(loaded.problems()).isEmpty();
        assertThat(loaded.isUsable()).isTrue();
        assertThat(loaded.hasIcon()).isTrue();
        assertThat(loaded.sha256()).hasSize(64);
        var voice = loaded.pack().orElseThrow().voice("anna").orElseThrow();
        VoiceLine hurt = voice.lines(EventCatalog.HURT).getFirst();
        assertThat(hurt.duration()).hasValueSatisfying(duration -> assertThat(duration).isCloseTo(0.5f, within(0.01f)));
        // a duration from the manifest wins over the measured one
        assertThat(voice.lines(EventCatalog.DEATH).getFirst().duration()).contains(2.5f);
    }

    @Test
    void sameFileHasSameHash() throws Exception {
        TestPacks pack = TestPacks.create().manifest(manifest("", BASIC_LINES)).sounds(BASIC_FILES);
        LoadedPack first = read(pack);
        LoadedPack second = PackReader.read(Files.copy(first.source(), dir.resolve("copy.voicepack")));

        assertThat(second.sha256()).isEqualTo(first.sha256());
    }

    @Test
    void reportsMissingManifest() throws Exception {
        LoadedPack loaded = read(TestPacks.create().sounds(BASIC_FILES));

        assertThat(loaded.isUsable()).isFalse();
        assertThat(loaded.errors()).extracting(Problem::code).containsExactly(ProblemCode.MISSING_MANIFEST);
    }

    @Test
    void reportsFilesThatAreNoZip() throws Exception {
        Path file = Files.writeString(dir.resolve("broken.voicepack"), "definitely not a zip");

        LoadedPack loaded = PackReader.read(file);

        assertThat(loaded.isUsable()).isFalse();
        assertThat(loaded.errors()).extracting(Problem::code).containsExactly(ProblemCode.UNREADABLE_ARCHIVE);
    }

    @Test
    void reportsMissingAndInvalidAudio() throws Exception {
        LoadedPack loaded = read(TestPacks.create()
                .manifest(manifest("", BASIC_LINES + """
                        , "greeting": [
                            { "file": "sounds/anna/greeting/missing.ogg" },
                            { "file": "sounds/anna/greeting/opus.ogg" },
                            { "file": "sounds/anna/greeting/wave.ogg" }
                          ]
                        """))
                .sounds(BASIC_FILES)
                .file("sounds/anna/greeting/opus.ogg", TestPacks.audio("opus.ogg"))
                .file("sounds/anna/greeting/wave.ogg", TestPacks.audio("not-ogg.wav")));

        assertThat(loaded.isUsable()).isFalse();
        assertThat(loaded.errors()).extracting(Problem::code, Problem::path).containsExactlyInAnyOrder(
                tuple(ProblemCode.MISSING_FILE, "voices[0].lines.greeting[0].file"),
                tuple(ProblemCode.NOT_VORBIS, "voices[0].lines.greeting[1].file"),
                tuple(ProblemCode.NOT_OGG, "voices[0].lines.greeting[2].file"));
    }

    @Test
    void warnsAboutLikelyMistakesButStillLoads() throws Exception {
        String voiceWithIssues = manifest("\"child\": true,", """
                "greeting": [{ "file": "sounds/anna/greeting/hi.ogg" }],
                "throw_potion": [{ "file": "sounds/anna/greeting/hi.ogg" }]
                """).replace("[\"villager\"]", "[\"villager\", \"witch\"]");
        LoadedPack loaded = read(TestPacks.create()
                .manifest(voiceWithIssues)
                .file("sounds/anna/greeting/hi.ogg", TestPacks.audio("stereo.ogg"))
                .sounds("sounds/anna/unused.ogg")
                .file("readme.txt", new byte[]{1}));

        assertThat(loaded.isUsable()).isTrue();
        assertThat(loaded.warnings()).extracting(Problem::code).containsExactlyInAnyOrder(
                ProblemCode.MISSING_BASIC_EVENT,
                ProblemCode.CHILD_VOICE_NOT_VILLAGER,
                ProblemCode.STEREO_FILE,
                ProblemCode.UNUSED_FILE);
    }

    @Test
    void warnsAboutEventsTheEntitiesNeverTrigger() throws Exception {
        LoadedPack loaded = read(TestPacks.create()
                .manifest(manifest("", BASIC_LINES + ", \"throw_potion\": [{ \"file\": \"sounds/anna/hurt/au.ogg\" }]"))
                .sounds(BASIC_FILES));

        assertThat(loaded.warnings()).extracting(Problem::code, Problem::path)
                .containsExactly(tuple(ProblemCode.EVENT_NOT_USED, "voices[0].lines.throw_potion"));
    }

    @Test
    void warnsAboutVoicesWithoutLines() throws Exception {
        LoadedPack loaded = read(TestPacks.create().manifest(manifest("", "")));

        assertThat(loaded.isUsable()).isTrue();
        assertThat(loaded.warnings()).extracting(Problem::code).containsExactly(ProblemCode.NO_LINES);
    }
}

package me.teajay.talkingvillagers.core.convert;

import me.teajay.talkingvillagers.core.TestPacks;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.io.ZipPackContents;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.Voice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Version1ConverterTest {
    private static final String ROOT = Version1Converter.RESOURCE_PACK_VOICES;

    @TempDir
    Path dir;

    private Path version1Pack() throws Exception {
        return TestPacks.create()
                .file("pack.mcmeta", "{}".getBytes(StandardCharsets.UTF_8))
                .sounds(ROOT + "teajay/all/hurt/hurt1.ogg", ROOT + "teajay/all/hurt/hurt2.ogg", ROOT + "teajay/all/death/death1.ogg",
                        ROOT + "teajay/all/greeting/hello1.ogg", ROOT + "teajay/farmer/random/crops1.ogg",
                        ROOT + "teajay/cartograph/random/maps1.ogg", ROOT + "teajay/all/herodrop/gift1.ogg",
                        ROOT + "teajay/all/nope/nope1.ogg", ROOT + "teajay/all/sneeze/a.ogg")
                .file(Version1Converter.RESOURCE_PACK_LANG, """
                        { "subtitles.talkingvillagers.teajay_hello": "Hello there!", "subtitles.talkingvillagers.teajay_crops": "" }
                        """.getBytes(StandardCharsets.UTF_8))
                .write(dir, "v1.zip");
    }

    @Test
    void convertsAResourcePackIntoAValidVoicepack() throws Exception {
        Version1Converter.Result result;
        try (ZipPackContents contents = new ZipPackContents(version1Pack())) {
            assertThat(Version1Converter.isVersion1(contents)).isTrue();
            result = Version1Converter.convertResourcePack(contents,
                    new Version1Converter.Settings("teajay", "TeaJay", "2.0.0", "en", Gender.MALE, List.of("TeaJay")));
        }
        Path target = dir.resolve("out/teajay.voicepack");
        Version1Converter.write(result, Map.of(), target);

        LoadedPack loaded = PackReader.read(target);
        assertThat(loaded.errors()).isEmpty();
        Voice voice = loaded.pack().orElseThrow().voice("teajay").orElseThrow();
        assertThat(voice.gender()).isEqualTo(Gender.MALE);
        assertThat(voice.legacyIds()).containsExactly("teajay");
        assertThat(voice.lines(EventCatalog.HURT)).hasSize(2);
        assertThat(voice.lines(EventCatalog.HERO_GIFT)).hasSize(1);
        assertThat(voice.lines(EventCatalog.NO)).hasSize(1);
        assertThat(voice.lines(EventCatalog.GREETING).getFirst().text()).contains("Hello there!");
        assertThat(voice.lines(EventCatalog.RANDOM)).extracting(line -> line.conditions().professions())
                .containsExactlyInAnyOrder(java.util.Set.of("farmer"), java.util.Set.of("cartographer"));
        assertThat(voice.lines(EventCatalog.RANDOM)).allSatisfy(line -> assertThat(line.text()).isEmpty());
        // durations come from the Ogg headers, the converter does not need them
        assertThat(voice.lines(EventCatalog.HURT).getFirst().duration()).isPresent();
        assertThat(result.skipped()).containsExactly(ROOT + "teajay/all/sneeze/a.ogg");
    }

    @Test
    void recognisesOnlyVersion1Packs() throws Exception {
        Path other = TestPacks.create().manifest("{}").write(dir, "other.zip");
        try (ZipPackContents contents = new ZipPackContents(other)) {
            assertThat(Version1Converter.isVersion1(contents)).isFalse();
        }
    }

    @Test
    void sanitizesNames() {
        assertThat(Version1Converter.sanitize("Hello World!")).isEqualTo("hello_world");
        assertThat(Version1Converter.sanitize("???")).isEqualTo("voice");
    }
}

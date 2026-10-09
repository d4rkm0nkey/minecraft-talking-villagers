package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.TestPacks;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OggInfoTest {
    @Test
    void readsMonoVorbis() {
        OggInfo info = OggInfo.probe(TestPacks.audio("mono.ogg")).orElseThrow();
        assertThat(info.codec()).isEqualTo(OggInfo.Codec.VORBIS);
        assertThat(info.channels()).isEqualTo(1);
        assertThat(info.sampleRate()).isEqualTo(22050);
        assertThat(info.duration()).hasValueSatisfying(duration -> assertThat(duration).isCloseTo(0.5f, within(0.01f)));
    }

    @Test
    void readsStereoVorbis() {
        OggInfo info = OggInfo.probe(TestPacks.audio("stereo.ogg")).orElseThrow();
        assertThat(info.channels()).isEqualTo(2);
        assertThat(info.duration()).hasValueSatisfying(duration -> assertThat(duration).isCloseTo(1.0f, within(0.01f)));
    }

    @Test
    void recognisesOpus() {
        OggInfo info = OggInfo.probe(TestPacks.audio("opus.ogg")).orElseThrow();
        assertThat(info.codec()).isEqualTo(OggInfo.Codec.OPUS);
        assertThat(info.duration()).hasValueSatisfying(duration -> assertThat(duration).isCloseTo(0.5f, within(0.02f)));
    }

    @Test
    void rejectsOtherFormats() {
        assertThat(OggInfo.probe(TestPacks.audio("not-ogg.wav"))).isEmpty();
        assertThat(OggInfo.probe(new byte[0])).isEmpty();
    }
}

package me.teajay.talkingvillagers.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguagesTest {
    @Test
    void validatesAndNormalisesTags() {
        assertThat(Languages.isValid("de")).isTrue();
        assertThat(Languages.isValid("en-GB")).isTrue();
        assertThat(Languages.isValid("en_us")).isFalse();
        assertThat(Languages.isValid("")).isFalse();
        assertThat(Languages.normalize("EN-gb")).isEqualTo("en-GB");
    }

    @Test
    void matchesByPrimaryLanguage() {
        assertThat(Languages.matches("en-GB", "en")).isTrue();
        assertThat(Languages.matches("en", "en-US")).isTrue();
        assertThat(Languages.matches("DE", "de")).isTrue();
        assertThat(Languages.matches("de", "en")).isFalse();
    }

    @Test
    void voiceRefRoundTrips() {
        VoiceRef ref = VoiceRef.parse("pack:anna").orElseThrow();
        assertThat(ref).isEqualTo(new VoiceRef("pack", "anna"));
        assertThat(ref.toString()).isEqualTo("pack:anna");
        assertThat(VoiceRef.parse("anna")).isEmpty();
        assertThat(VoiceRef.parse("a:b:c")).isEmpty();
        assertThat(VoiceRef.parse(":b")).isEmpty();
    }
}

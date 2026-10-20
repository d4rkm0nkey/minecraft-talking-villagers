package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceIdentityTest {
    private static final VoiceIdentity MAN = VoiceIdentity.withoutVoice(Gender.MALE, false, Optional.of("en"))
            .withVoice(new VoiceRef("pack", "anna"), 1.1f, false, true);

    @Test
    void takesOverTheGenderOfTheVoice() {
        VoiceIdentity identity = MAN.adoptingGenderOf(Gender.FEMALE);

        assertThat(identity.gender()).isEqualTo(Gender.FEMALE);
        assertThat(identity.voice()).isEqualTo(MAN.voice());
        assertThat(identity.pitch()).isEqualTo(1.1f);
        assertThat(identity.manual()).isTrue();
        assertThat(identity.language()).contains("en");
    }

    @Test
    void neutralVoicesKeepTheGender() {
        assertThat(MAN.adoptingGenderOf(Gender.NEUTRAL)).isEqualTo(MAN);
    }
}

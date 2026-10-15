package me.teajay.talkingvillagers.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionsTest {
    @Test
    void comparesNumericPartsAsNumbers() {
        assertThat(Versions.compare("1.10.0", "1.9.0")).isPositive();
        assertThat(Versions.compare("2", "10")).isNegative();
        assertThat(Versions.compare("1.0", "1.0.0")).isZero();
    }

    @Test
    void releaseIsNewerThanPreRelease() {
        assertThat(Versions.compare("2.0.0", "2.0.0-beta.2")).isPositive();
        assertThat(Versions.compare("2.0.0-beta.10", "2.0.0-beta.2")).isPositive();
        assertThat(Versions.compare("2.0.0-alpha", "2.0.0-beta")).isNegative();
    }

    @Test
    void ignoresBuildMetadataAndHandlesText() {
        assertThat(Versions.compare("1.0.0+abc", "1.0.0+def")).isZero();
        assertThat(Versions.compare("v2", "v1")).isPositive();
    }
}

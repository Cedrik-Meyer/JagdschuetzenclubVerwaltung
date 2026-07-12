package de.jsc.kasse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Smoke-Test: bestätigt lediglich, dass die Testinfrastruktur (JUnit 5 + AssertJ)
 * korrekt eingerichtet ist und läuft.
 */
class SmokeTest {

    @Test
    void testinfrastrukturLaeuft() {
        assertThat(1 + 1).isEqualTo(2);
    }
}

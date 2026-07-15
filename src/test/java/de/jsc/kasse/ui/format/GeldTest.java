package de.jsc.kasse.ui.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeldTest {

    @Test
    void formatiereGaengigeBetraege() {
        assertThat(Geld.formatiere(0)).isEqualTo("0,00 €");
        assertThat(Geld.formatiere(5)).isEqualTo("0,05 €");
        assertThat(Geld.formatiere(99)).isEqualTo("0,99 €");
        assertThat(Geld.formatiere(100)).isEqualTo("1,00 €");
        assertThat(Geld.formatiere(1234)).isEqualTo("12,34 €");
        assertThat(Geld.formatiere(-1234)).isEqualTo("-12,34 €");
    }

    @Test
    void parseAkzeptiertKommaPunktUndEuroZeichen() {
        assertThat(Geld.parse("12,34")).isEqualTo(1234);
        assertThat(Geld.parse("12.34")).isEqualTo(1234);
        assertThat(Geld.parse("12")).isEqualTo(1200);
        assertThat(Geld.parse("12,5")).isEqualTo(1250);
        assertThat(Geld.parse("0,99")).isEqualTo(99);
        assertThat(Geld.parse("  12,34 € ")).isEqualTo(1234);
        assertThat(Geld.parse("-5")).isEqualTo(-500);
    }

    @Test
    void parseLehntUngueltigeEingabenAb() {
        assertThatThrownBy(() -> Geld.parse("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geld.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geld.parse("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Geld.parse("1,234")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatierenUndParsenSindInvers() {
        for (long cent : new long[] {0, 5, 99, 100, 1234, 250050}) {
            assertThat(Geld.parse(Geld.formatiere(cent))).isEqualTo(cent);
        }
    }
}

package de.jsc.kasse.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.Abrechnungsart;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Abgestimmte Testfälle T1–T11 der Preis-Engine (ARCHITEKTUR.md 5.1).
 *
 * <p>Stand-IDs entsprechen der Seed-Reihenfolge: Skeet=1, Trap=2, Kugel=4, Pistole=5.
 */
class PreisRechnerTest {

    private static final long SKEET = 1;
    private static final long TRAP = 2;
    private static final long KUGEL = 4;
    private static final long PISTOLE = 5;

    private final PreisRechner rechner = new PreisRechner();

    private static PositionsWunsch proTaube(long standId, int menge, long preisM, long preisG) {
        return new PositionsWunsch(standId, Abrechnungsart.PRO_TAUBE, menge, preisM, preisG);
    }

    private static PositionsWunsch pauschal(long standId, int menge, long preisM, long preisG) {
        return new PositionsWunsch(standId, Abrechnungsart.PAUSCHAL, menge, preisM, preisG);
    }

    @Test
    void t1_leereListe() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of());

        assertThat(ergebnis.zeilen()).isEmpty();
        assertThat(ergebnis.gesamtCent()).isZero();
    }

    @Test
    void t2_mitgliedProTaube() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of(proTaube(SKEET, 25, 12, 15)));

        assertThat(ergebnis.zeilen()).containsExactly(new Preiszeile(SKEET, 25, 12, 300));
        assertThat(ergebnis.gesamtCent()).isEqualTo(300);
    }

    @Test
    void t3_gastProTaube() {
        Preisaufstellung ergebnis = rechner.berechne(false, List.of(proTaube(SKEET, 25, 12, 15)));

        assertThat(ergebnis.zeilen()).containsExactly(new Preiszeile(SKEET, 25, 15, 375));
        assertThat(ergebnis.gesamtCent()).isEqualTo(375);
    }

    @Test
    void t4_mitgliedPauschal() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of(pauschal(KUGEL, 1, 500, 700)));

        assertThat(ergebnis.zeilen()).containsExactly(new Preiszeile(KUGEL, 1, 500, 500));
        assertThat(ergebnis.gesamtCent()).isEqualTo(500);
    }

    @Test
    void t5_pauschalMengeEgal() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of(pauschal(KUGEL, 3, 500, 700)));

        assertThat(ergebnis.zeilen()).containsExactly(new Preiszeile(KUGEL, 1, 500, 500));
        assertThat(ergebnis.gesamtCent()).isEqualTo(500);
    }

    @Test
    void t6_pauschalGast() {
        Preisaufstellung ergebnis = rechner.berechne(false, List.of(pauschal(KUGEL, 3, 500, 700)));

        assertThat(ergebnis.zeilen()).containsExactly(new Preiszeile(KUGEL, 1, 700, 700));
        assertThat(ergebnis.gesamtCent()).isEqualTo(700);
    }

    @Test
    void t7_gemischtMitglied() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of(
                proTaube(SKEET, 25, 12, 15),
                proTaube(TRAP, 50, 10, 13),
                pauschal(KUGEL, 2, 500, 700)));

        assertThat(ergebnis.zeilen()).containsExactly(
                new Preiszeile(SKEET, 25, 12, 300),
                new Preiszeile(TRAP, 50, 10, 500),
                new Preiszeile(KUGEL, 1, 500, 500));
        assertThat(ergebnis.gesamtCent()).isEqualTo(1300);
    }

    @Test
    void t8_gemischtGast() {
        Preisaufstellung ergebnis = rechner.berechne(false, List.of(
                proTaube(SKEET, 25, 12, 15),
                proTaube(TRAP, 50, 10, 13),
                pauschal(KUGEL, 2, 500, 700)));

        assertThat(ergebnis.zeilen()).containsExactly(
                new Preiszeile(SKEET, 25, 15, 375),
                new Preiszeile(TRAP, 50, 13, 650),
                new Preiszeile(KUGEL, 1, 700, 700));
        assertThat(ergebnis.gesamtCent()).isEqualTo(1725);
    }

    @Test
    void t9_zweiPauschalStaende() {
        Preisaufstellung ergebnis = rechner.berechne(true, List.of(
                pauschal(KUGEL, 1, 500, 700),
                pauschal(PISTOLE, 1, 400, 600)));

        assertThat(ergebnis.zeilen()).containsExactly(
                new Preiszeile(KUGEL, 1, 500, 500),
                new Preiszeile(PISTOLE, 1, 400, 400));
        assertThat(ergebnis.gesamtCent()).isEqualTo(900);
    }

    @Test
    void t10_mengeKleiner1() {
        List<PositionsWunsch> positionen = List.of(proTaube(SKEET, 0, 12, 15));

        assertThatThrownBy(() -> rechner.berechne(true, positionen))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void t11_negativerPreis() {
        List<PositionsWunsch> positionen = List.of(proTaube(SKEET, 25, -1, 15));

        assertThatThrownBy(() -> rechner.berechne(true, positionen))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

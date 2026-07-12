package de.jsc.kasse.lizenz;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.LizenzBewertung;
import de.jsc.kasse.domain.Schiesserlaubnis;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Abgestimmte Testfälle L1–L12 des Lizenz-Prüfers (ARCHITEKTUR.md 5.2).
 * Stichtag 15.06.2026, außer wo angegeben. {@code tageBisAblauf} exakt nur, wo eindeutig,
 * sonst Vorzeichen.
 */
class LizenzprueferTest {

    private static final LocalDate STICHTAG = LocalDate.of(2026, 6, 15);

    private final LizenzPruefer pruefer = new LizenzPruefer();

    private static Schiesserlaubnis erlaubnis(LizenzArt art, LocalDate ablauf) {
        return new Schiesserlaubnis(0, 1, art, null, ablauf, null);
    }

    @Test
    void l1_keineErlaubnis_fehlt() {
        LizenzStatus status = pruefer.pruefe(List.of(), null, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.FEHLT, null, 0, false));
    }

    @Test
    void l2_gueltigFolgejahr_ohneWarnung() {
        LocalDate ablauf = LocalDate.of(2027, 3, 31);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, ablauf)), null, STICHTAG);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(ablauf);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isFalse();
    }

    @Test
    void l3_ablaufHeute_gueltigMitWarnung() {
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, STICHTAG)), null, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.GUELTIG, STICHTAG, 0, true));
    }

    @Test
    void l4_gesternAbgelaufen() {
        LocalDate ablauf = LocalDate.of(2026, 6, 14);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, ablauf)), null, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.ABGELAUFEN, ablauf, -1, false));
    }

    @Test
    void l5_zweiGleicheArtEineGueltig_gueltig() {
        LocalDate gueltig = LocalDate.of(2027, 3, 31);
        LizenzStatus status = pruefer.pruefe(List.of(
                erlaubnis(LizenzArt.JAGDSCHEIN, LocalDate.of(2026, 6, 14)),
                erlaubnis(LizenzArt.JAGDSCHEIN, gueltig)), null, STICHTAG);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(gueltig);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isFalse();
    }

    @Test
    void l6_alleAbgelaufen_spaetestesRelevantesDatum() {
        LocalDate spaeter = LocalDate.of(2026, 6, 14);
        LizenzStatus status = pruefer.pruefe(List.of(
                erlaubnis(LizenzArt.JAGDSCHEIN, LocalDate.of(2026, 1, 1)),
                erlaubnis(LizenzArt.SPORTSCHUETZENSCHEIN, spaeter)), null, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.ABGELAUFEN, spaeter, -1, false));
    }

    @Test
    void l7_baldFaellig_gueltigMitWarnung() {
        LocalDate ablauf = LocalDate.of(2026, 6, 25);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.WBK, ablauf)), null, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.GUELTIG, ablauf, 10, true));
    }

    @Test
    void l8_verschiedeneArtenSpaetereGueltig_gueltigMitWarnung() {
        LocalDate gueltig = LocalDate.of(2026, 12, 31);
        LizenzStatus status = pruefer.pruefe(List.of(
                erlaubnis(LizenzArt.JAGDSCHEIN, LocalDate.of(2026, 1, 1)),
                erlaubnis(LizenzArt.WBK, gueltig)), null, STICHTAG);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(gueltig);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isTrue();
    }

    @Test
    void l9_vorJahreswechsel_keinWarnen() {
        LocalDate stichtag = LocalDate.of(2025, 12, 31);
        LocalDate ablauf = LocalDate.of(2026, 3, 31);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, ablauf)), null, stichtag);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(ablauf);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isFalse();
    }

    @Test
    void l10_abJahresbeginn_warnung() {
        LocalDate stichtag = LocalDate.of(2026, 1, 1);
        LocalDate ablauf = LocalDate.of(2026, 3, 31);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, ablauf)), null, stichtag);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(ablauf);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isTrue();
    }

    @Test
    void l11_geforderteArtGueltig_gueltig() {
        LocalDate ablauf = LocalDate.of(2027, 3, 31);
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.JAGDSCHEIN, ablauf)), LizenzArt.JAGDSCHEIN, STICHTAG);

        assertThat(status.bewertung()).isEqualTo(LizenzBewertung.GUELTIG);
        assertThat(status.ablaufdatum()).isEqualTo(ablauf);
        assertThat(status.tageBisAblauf()).isPositive();
        assertThat(status.warnung()).isFalse();
    }

    @Test
    void l12_geforderteArtFehlt_fehlt() {
        LizenzStatus status = pruefer.pruefe(
                List.of(erlaubnis(LizenzArt.WBK, LocalDate.of(2026, 12, 31))),
                LizenzArt.JAGDSCHEIN, STICHTAG);

        assertThat(status).isEqualTo(new LizenzStatus(LizenzBewertung.FEHLT, null, 0, false));
    }
}

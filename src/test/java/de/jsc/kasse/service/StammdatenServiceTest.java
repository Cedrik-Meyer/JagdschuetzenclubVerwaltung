package de.jsc.kasse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.TarifRepositoryJdbc;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StammdatenServiceTest {

    private Datenbank db;
    private StammdatenService service;
    private long skeetId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        StandRepositoryJdbc standRepository = new StandRepositoryJdbc(db.verbindung());
        service = new StammdatenService(standRepository, new TarifRepositoryJdbc(db.verbindung()));
        skeetId = standRepository.findeByName("Skeet").orElseThrow().id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void alleStaendeNurAktiveBeruecksichtigtDeaktivierung() {
        assertThat(service.alleStaende(false)).hasSize(6);

        service.standAktivSetzen(skeetId, false);

        assertThat(service.alleStaende(false)).hasSize(6);
        assertThat(service.alleStaende(true)).hasSize(5)
                .extracting(Stand::name).doesNotContain("Skeet");
    }

    @Test
    void aktuellerTarif_a_genauAmGueltigAb() {
        Tarif tarif = service.neuenTarifAnlegen(skeetId, 12, 15, LocalDate.of(2026, 1, 1));

        assertThat(service.aktuellerTarif(skeetId, LocalDate.of(2026, 1, 1))).isEqualTo(tarif);
    }

    @Test
    void aktuellerTarif_b_neuesterVorOderAmDatumGewinnt() {
        Tarif alt = service.neuenTarifAnlegen(skeetId, 10, 13, LocalDate.of(2025, 1, 1));
        Tarif neu = service.neuenTarifAnlegen(skeetId, 12, 15, LocalDate.of(2026, 1, 1));

        assertThat(service.aktuellerTarif(skeetId, LocalDate.of(2026, 6, 15))).isEqualTo(neu);
        assertThat(service.aktuellerTarif(skeetId, LocalDate.of(2025, 6, 1))).isEqualTo(alt);
    }

    @Test
    void aktuellerTarif_c_datumVorJedemTarif_wirftIllegalState() {
        service.neuenTarifAnlegen(skeetId, 12, 15, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.aktuellerTarif(skeetId, LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Skeet");
    }

    @Test
    void aktuellerTarif_d_unbekannterStand_wirftAusnahme() {
        assertThatThrownBy(() -> service.aktuellerTarif(999_999L, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void neuenTarifAnlegenBehaeltHistorieUndTarifHistorieIstAbsteigend() {
        service.neuenTarifAnlegen(skeetId, 10, 13, LocalDate.of(2025, 1, 1));
        service.neuenTarifAnlegen(skeetId, 12, 15, LocalDate.of(2026, 1, 1));

        assertThat(service.tarifHistorie(skeetId))
                .extracting(Tarif::gueltigAb)
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2025, 1, 1));
    }

    @Test
    void negativerPreisWirftAusnahme() {
        assertThatThrownBy(() -> service.neuenTarifAnlegen(skeetId, -1, 15, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standAktivSetzenUnbekannterStand_wirftAusnahme() {
        assertThatThrownBy(() -> service.standAktivSetzen(999_999L, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

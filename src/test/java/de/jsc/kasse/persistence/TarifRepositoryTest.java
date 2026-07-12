package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.TarifRepositoryJdbc;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TarifRepositoryTest {

    private Datenbank db;
    private TarifRepository repo;
    private long standId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new TarifRepositoryJdbc(db.verbindung());
        Stand skeet = new StandRepositoryJdbc(db.verbindung()).findeByName("Skeet").orElseThrow();
        standId = skeet.id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void anlegenUndLesenMitPreisenInCent() {
        Tarif gespeichert = repo.anlegen(new Tarif(0, standId, 12, 15, LocalDate.of(2026, 1, 1)));

        assertThat(gespeichert.id()).isPositive();
        assertThat(repo.findeById(gespeichert.id())).get()
                .satisfies(t -> {
                    assertThat(t.preisMitgliedCent()).isEqualTo(12);
                    assertThat(t.preisGastCent()).isEqualTo(15);
                });
    }

    @Test
    void findeByStandLiefertHistorieAufsteigendNachGueltigAb() {
        repo.anlegen(new Tarif(0, standId, 12, 15, LocalDate.of(2026, 1, 1)));
        repo.anlegen(new Tarif(0, standId, 13, 16, LocalDate.of(2025, 1, 1)));

        assertThat(repo.findeByStand(standId))
                .extracting(Tarif::gueltigAb)
                .containsExactly(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1));
    }
}

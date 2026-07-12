package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.Abrechnungsart;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StandRepositoryTest {

    private Datenbank db;
    private StandRepository repo;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new StandRepositoryJdbc(db.verbindung());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void seedStaendeSindVorhanden() {
        assertThat(repo.findeAlle()).hasSize(6);
        assertThat(repo.findeByName("Trap")).get()
                .extracting(Stand::abrechnungsart).isEqualTo(Abrechnungsart.PRO_TAUBE);
    }

    @Test
    void anlegenNeuerStand() {
        Stand gespeichert = repo.anlegen(new Stand(0, "Bogen", Abrechnungsart.PAUSCHAL, null, true));

        assertThat(gespeichert.id()).isPositive();
        assertThat(repo.findeAlle()).hasSize(7);
        assertThat(repo.findeByName("Bogen")).contains(gespeichert);
    }

    @Test
    void aktualisierenDeaktiviertStand() {
        Stand skeet = repo.findeByName("Skeet").orElseThrow();

        repo.aktualisieren(new Stand(skeet.id(), skeet.name(), skeet.abrechnungsart(),
                skeet.erforderlicheLizenz(), false));

        assertThat(repo.findeById(skeet.id())).get()
                .extracting(Stand::aktiv).isEqualTo(false);
    }
}

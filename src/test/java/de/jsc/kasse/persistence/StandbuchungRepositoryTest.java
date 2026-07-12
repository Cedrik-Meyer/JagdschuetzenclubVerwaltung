package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.persistence.jdbc.BesuchRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandbuchungRepositoryJdbc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StandbuchungRepositoryTest {

    private Datenbank db;
    private StandbuchungRepository repo;
    private long besuchId;
    private long standId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new StandbuchungRepositoryJdbc(db.verbindung());

        Person person = new PersonRepositoryJdbc(db.verbindung()).anlegen(
                new Person(0, PersonTyp.MITGLIED, "Emil", "Eins", null, "M-1", null,
                        true, null, null, LocalDateTime.of(2026, 1, 1, 8, 0)));
        Besuch besuch = new BesuchRepositoryJdbc(db.verbindung()).anlegen(
                new Besuch(0, person.id(), LocalDate.of(2026, 7, 12),
                        LocalDateTime.of(2026, 7, 12, 9, 0), null, BesuchStatus.ANGEMELDET,
                        0, false, null, null));
        Stand skeet = new StandRepositoryJdbc(db.verbindung()).findeByName("Skeet").orElseThrow();

        besuchId = besuch.id();
        standId = skeet.id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void anlegenFriertSnapshotPreiseEinUndIstLesbar() {
        // 25 Tauben * 12 ct = 300 ct — Snapshot-Werte.
        Standbuchung gespeichert = repo.anlegen(new Standbuchung(0, besuchId, standId, 25, 12, 300));

        assertThat(gespeichert.id()).isPositive();
        assertThat(repo.findeById(gespeichert.id())).get().satisfies(sb -> {
            assertThat(sb.menge()).isEqualTo(25);
            assertThat(sb.einzelpreisCent()).isEqualTo(12);
            assertThat(sb.zeilenpreisCent()).isEqualTo(300);
        });
    }

    @Test
    void findeByBesuchLiefertAlleZeilenInReihenfolge() {
        repo.anlegen(new Standbuchung(0, besuchId, standId, 25, 12, 300));
        repo.anlegen(new Standbuchung(0, besuchId, standId, 50, 10, 500));

        assertThat(repo.findeByBesuch(besuchId))
                .extracting(Standbuchung::zeilenpreisCent)
                .containsExactly(300L, 500L);
    }
}

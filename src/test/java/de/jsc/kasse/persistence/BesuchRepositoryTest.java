package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.persistence.jdbc.BesuchRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BesuchRepositoryTest {

    private Datenbank db;
    private BesuchRepository repo;
    private long personId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new BesuchRepositoryJdbc(db.verbindung());
        Person person = new PersonRepositoryJdbc(db.verbindung()).anlegen(
                new Person(0, PersonTyp.GAST, "Dora", "Doppel", null, null, null,
                        true, null, null, LocalDateTime.of(2026, 1, 1, 8, 0)));
        personId = person.id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Besuch offenerBesuch() {
        return new Besuch(0, personId, LocalDate.of(2026, 7, 12),
                LocalDateTime.of(2026, 7, 12, 9, 30), null, BesuchStatus.ANGEMELDET,
                0, false, null);
    }

    @Test
    void anlegenUndFindeByPerson() {
        Besuch gespeichert = repo.anlegen(offenerBesuch());

        assertThat(gespeichert.id()).isPositive();
        assertThat(repo.findeByPerson(personId)).containsExactly(gespeichert);
    }

    @Test
    void abmeldenSpeichertCheckOutGesamtSnapshotUndBezahlung() {
        Besuch gespeichert = repo.anlegen(offenerBesuch());

        Besuch abgemeldet = new Besuch(gespeichert.id(), personId, gespeichert.datum(),
                gespeichert.checkIn(), LocalDateTime.of(2026, 7, 12, 12, 0),
                BesuchStatus.ABGEMELDET, 1725, true, LocalDateTime.of(2026, 7, 12, 12, 1));
        repo.aktualisieren(abgemeldet);

        assertThat(repo.findeById(gespeichert.id())).get().satisfies(b -> {
            assertThat(b.status()).isEqualTo(BesuchStatus.ABGEMELDET);
            assertThat(b.checkOut()).isEqualTo(LocalDateTime.of(2026, 7, 12, 12, 0));
            assertThat(b.gesamtCent()).isEqualTo(1725);
            assertThat(b.bezahlt()).isTrue();
            assertThat(b.bezahltAm()).isEqualTo(LocalDateTime.of(2026, 7, 12, 12, 1));
        });
    }
}

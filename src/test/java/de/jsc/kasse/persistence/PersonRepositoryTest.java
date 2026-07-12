package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonRepositoryTest {

    private Datenbank db;
    private PersonRepository repo;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new PersonRepositoryJdbc(db.verbindung());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private static Person neuesMitglied() {
        return new Person(0, PersonTyp.MITGLIED, "Anna", "Schütz",
                LocalDate.of(1985, 5, 20), "M-001", LocalDate.of(2010, 1, 1),
                true, null, "anna@example.com", LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    @Test
    void anlegenVergibtIdUndIstLesbar() {
        Person gespeichert = repo.anlegen(neuesMitglied());

        assertThat(gespeichert.id()).isPositive();
        assertThat(repo.findeById(gespeichert.id())).contains(gespeichert);
    }

    @Test
    void findeAlleLiefertAngelegtePersonen() {
        repo.anlegen(neuesMitglied());
        assertThat(repo.findeAlle()).hasSize(1);
    }

    @Test
    void aktualisierenDeaktiviertOhneHartesLoeschen() {
        Person gespeichert = repo.anlegen(neuesMitglied());

        repo.aktualisieren(new Person(gespeichert.id(), gespeichert.typ(), gespeichert.vorname(),
                gespeichert.nachname(), gespeichert.geburtsdatum(), gespeichert.mitgliedsnummer(),
                gespeichert.beitrittsdatum(), false, gespeichert.eingeladenVon(),
                gespeichert.kontakt(), gespeichert.angelegtAm()));

        assertThat(repo.findeById(gespeichert.id())).get()
                .extracting(Person::aktiv).isEqualTo(false);
        assertThat(repo.findeAlle()).hasSize(1); // weiterhin vorhanden
    }

    @Test
    void gastMitEingeladenVonWirdPersistiert() {
        Person mitglied = repo.anlegen(neuesMitglied());

        Person gast = repo.anlegen(new Person(0, PersonTyp.GAST, "Ben", "Gast",
                null, null, null, true, mitglied.id(), null,
                LocalDateTime.of(2026, 1, 2, 10, 0)));

        assertThat(repo.findeById(gast.id())).get()
                .extracting(Person::eingeladenVon).isEqualTo(mitglied.id());
    }

    @Test
    void doppelteMitgliedsnummerVerletztUniqueConstraint() {
        repo.anlegen(neuesMitglied());
        assertThatThrownBy(() -> repo.anlegen(neuesMitglied()))
                .isInstanceOf(PersistenzException.class);
    }
}

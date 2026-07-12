package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.SchiesserlaubnisRepositoryJdbc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchiesserlaubnisRepositoryTest {

    private Datenbank db;
    private SchiesserlaubnisRepository repo;
    private long personId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        repo = new SchiesserlaubnisRepositoryJdbc(db.verbindung());
        Person person = new PersonRepositoryJdbc(db.verbindung()).anlegen(
                new Person(0, PersonTyp.MITGLIED, "Carla", "Kugel", null, "M-9", null,
                        true, null, null, LocalDateTime.of(2026, 1, 1, 8, 0)));
        personId = person.id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void anlegenUndFindeByPerson() {
        Schiesserlaubnis erlaubnis = repo.anlegen(new Schiesserlaubnis(0, personId,
                LizenzArt.JAGDSCHEIN, "JS-123", LocalDate.of(2027, 3, 1), LocalDate.of(2024, 3, 1)));

        assertThat(erlaubnis.id()).isPositive();
        assertThat(repo.findeByPerson(personId)).containsExactly(erlaubnis);
    }

    @Test
    void aktualisierenSetztNeuesAblaufdatum() {
        Schiesserlaubnis erlaubnis = repo.anlegen(new Schiesserlaubnis(0, personId,
                LizenzArt.WBK, null, LocalDate.of(2025, 1, 1), null));

        repo.aktualisieren(new Schiesserlaubnis(erlaubnis.id(), personId, LizenzArt.WBK, null,
                LocalDate.of(2030, 1, 1), null));

        assertThat(repo.findeById(erlaubnis.id())).get()
                .extracting(Schiesserlaubnis::ablaufdatum).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void ungueltigePersonReferenzVerletztFremdschluessel() {
        Schiesserlaubnis ohnePerson = new Schiesserlaubnis(0, 999_999L,
                LizenzArt.JAGDSCHEIN, null, LocalDate.of(2027, 1, 1), null);

        assertThatThrownBy(() -> repo.anlegen(ohnePerson))
                .isInstanceOf(PersistenzException.class);
    }
}

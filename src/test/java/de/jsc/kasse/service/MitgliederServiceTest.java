package de.jsc.kasse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.PersonRepository;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.SchiesserlaubnisRepositoryJdbc;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MitgliederServiceTest {

    private Datenbank db;
    private PersonRepository personRepository;
    private MitgliederService service;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        personRepository = new PersonRepositoryJdbc(db.verbindung());
        service = new MitgliederService(personRepository,
                new SchiesserlaubnisRepositoryJdbc(db.verbindung()));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Person mitglied() {
        return service.anlegenMitglied("Anna", "Schütz", "M-001",
                LocalDate.of(1985, 5, 20), LocalDate.of(2010, 1, 1), "anna@example.com");
    }

    @Test
    void anlegenMitgliedSetztTypAktivUndAngelegtAm() {
        Person m = mitglied();

        assertThat(m.id()).isPositive();
        assertThat(m.typ()).isEqualTo(PersonTyp.MITGLIED);
        assertThat(m.aktiv()).isTrue();
        assertThat(m.angelegtAm()).isNotNull();
        assertThat(service.findeMitgliedNachNummer("M-001")).contains(m);
    }

    @Test
    void doppelteMitgliedsnummerWirftAusnahme() {
        mitglied();
        assertThatThrownBy(() -> service.anlegenMitglied("Bea", "Berg", "M-001", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fehlendeMitgliedsnummerWirftAusnahme() {
        assertThatThrownBy(() -> service.anlegenMitglied("Bea", "Berg", "  ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sucheFindetNachNameTeilstringUndNummer() {
        Person m = mitglied();

        assertThat(service.sucheMitglied("schü")).contains(m);   // case-insensitiver Teilstring
        assertThat(service.sucheMitglied("ANN")).contains(m);
        assertThat(service.sucheMitglied("M-001")).contains(m);
        assertThat(service.sucheMitglied("Meier")).isEmpty();
    }

    @Test
    void anlegenGastMitEinladendemMitglied() {
        Person m = mitglied();

        Person gast = service.anlegenGast("Ben", "Gast", null, null, m.id());

        assertThat(gast.typ()).isEqualTo(PersonTyp.GAST);
        assertThat(gast.eingeladenVon()).isEqualTo(m.id());
        assertThat(gast.mitgliedsnummer()).isNull();
    }

    @Test
    void anlegenGastMitUnbekanntemEinladerWirftAusnahme() {
        assertThatThrownBy(() -> service.anlegenGast("Ben", "Gast", null, null, 999_999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anlegenGastMitGastAlsEinladerWirftAusnahme() {
        Person gast = service.anlegenGast("Ben", "Gast", null, null, null);

        assertThatThrownBy(() -> service.anlegenGast("Cara", "Gast", null, null, gast.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deaktivierePersonLoeschtNichtSondernSetztAktivFalse() {
        Person m = mitglied();

        service.deaktivierePerson(m.id());

        assertThat(personRepository.findeById(m.id())).get()
                .extracting(Person::aktiv).isEqualTo(false);
        assertThat(personRepository.findeAlle()).hasSize(1); // weiterhin vorhanden
    }

    @Test
    void aktualisierePersonAendertStammdaten() {
        Person m = mitglied();

        Person geaendert = new Person(m.id(), m.typ(), m.vorname(), "Neuname", m.geburtsdatum(),
                m.mitgliedsnummer(), m.beitrittsdatum(), m.aktiv(), m.eingeladenVon(),
                "neu@example.com", m.angelegtAm());
        service.aktualisierePerson(geaendert);

        assertThat(personRepository.findeById(m.id())).get().satisfies(p -> {
            assertThat(p.nachname()).isEqualTo("Neuname");
            assertThat(p.kontakt()).isEqualTo("neu@example.com");
        });
    }

    @Test
    void lizenzHinzufuegenUndLizenzenVon() {
        Person m = mitglied();

        service.lizenzHinzufuegen(m.id(), LizenzArt.JAGDSCHEIN, "JS-1",
                LocalDate.of(2027, 3, 1), LocalDate.of(2024, 3, 1));

        assertThat(service.lizenzenVon(m.id())).singleElement()
                .extracting(Schiesserlaubnis::art).isEqualTo(LizenzArt.JAGDSCHEIN);
    }

    @Test
    void lizenzAktualisierenTraegtNeuesAblaufdatumEin() {
        Person m = mitglied();
        Schiesserlaubnis lizenz = service.lizenzHinzufuegen(m.id(), LizenzArt.WBK, null,
                LocalDate.of(2025, 1, 1), null);

        Schiesserlaubnis neu = service.lizenzAktualisieren(lizenz.id(), LocalDate.of(2030, 1, 1));

        assertThat(neu.ablaufdatum()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(service.lizenzenVon(m.id())).singleElement()
                .extracting(Schiesserlaubnis::ablaufdatum).isEqualTo(LocalDate.of(2030, 1, 1));
    }
}

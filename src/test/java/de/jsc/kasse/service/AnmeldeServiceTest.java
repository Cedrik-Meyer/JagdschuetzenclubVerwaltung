package de.jsc.kasse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.LizenzBewertung;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.lizenz.LizenzPruefer;
import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.jdbc.BesuchRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.SchiesserlaubnisRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandbuchungRepositoryJdbc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnmeldeServiceTest {

    private Datenbank db;
    private PersonRepositoryJdbc personRepository;
    private SchiesserlaubnisRepositoryJdbc schiesserlaubnisRepository;
    private StandbuchungRepositoryJdbc standbuchungRepository;
    private StandRepositoryJdbc standRepository;
    private AnmeldeService service;

    private long skeetId;
    private long trapId;
    private long kugelId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        personRepository = new PersonRepositoryJdbc(db.verbindung());
        schiesserlaubnisRepository = new SchiesserlaubnisRepositoryJdbc(db.verbindung());
        standbuchungRepository = new StandbuchungRepositoryJdbc(db.verbindung());
        standRepository = new StandRepositoryJdbc(db.verbindung());
        service = new AnmeldeService(new BesuchRepositoryJdbc(db.verbindung()),
                standbuchungRepository, standRepository, schiesserlaubnisRepository,
                new LizenzPruefer());

        skeetId = standRepository.findeByName("Skeet").orElseThrow().id();
        trapId = standRepository.findeByName("Trap").orElseThrow().id();
        kugelId = standRepository.findeByName("Kugel").orElseThrow().id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private long mitgliedMit(LocalDate lizenzAblauf) {
        Person m = personRepository.anlegen(new Person(0, PersonTyp.MITGLIED, "Anna", "Schütz",
                null, "M-" + System.nanoTime(), null, true, null, null, LocalDateTime.now()));
        schiesserlaubnisRepository.anlegen(new Schiesserlaubnis(0, m.id(), LizenzArt.JAGDSCHEIN,
                "JS", lizenzAblauf, null));
        return m.id();
    }

    @Test
    void anmeldenLegtOffenenBesuchUndProvisorischeBuchungenAn() {
        long personId = mitgliedMit(LocalDate.now().plusYears(1));

        Besuch besuch = service.anmelden(personId, List.of(skeetId, kugelId), null);

        assertThat(besuch.status()).isEqualTo(BesuchStatus.ANGEMELDET);
        assertThat(besuch.gesamtCent()).isZero();
        assertThat(besuch.bezahlt()).isFalse();
        assertThat(besuch.lizenzVermerk()).isNull();

        List<Standbuchung> buchungen = standbuchungRepository.findeByBesuch(besuch.id());
        assertThat(buchungen).hasSize(2);
        assertThat(buchungen).allMatch(sb -> sb.einzelpreisCent() == 0 && sb.zeilenpreisCent() == 0);
        assertThat(buchungen).filteredOn(sb -> sb.standId() == skeetId)
                .allMatch(sb -> sb.menge() == 0);   // PRO_TAUBE provisorisch
        assertThat(buchungen).filteredOn(sb -> sb.standId() == kugelId)
                .allMatch(sb -> sb.menge() == 1);   // PAUSCHAL

        assertThat(service.offeneBesuche()).extracting(Besuch::id).contains(besuch.id());
    }

    @Test
    void lizenzPruefenLiefertGueltigFuerGueltigeLizenz() {
        long personId = mitgliedMit(LocalDate.now().plusYears(1));

        assertThat(service.lizenzPruefen(personId, LocalDate.now()).bewertung())
                .isEqualTo(LizenzBewertung.GUELTIG);
    }

    @Test
    void zweiteAnmeldungBeiOffenemBesuchWirftException() {
        long personId = mitgliedMit(LocalDate.now().plusYears(1));
        service.anmelden(personId, List.of(skeetId), null);

        assertThatThrownBy(() -> service.anmelden(personId, List.of(trapId), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ungueltigeLizenzOhneVermerkWirftException() {
        long personId = mitgliedMit(LocalDate.now().minusDays(1)); // abgelaufen

        assertThatThrownBy(() -> service.anmelden(personId, List.of(skeetId), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ungueltigeLizenzMitVermerkErlaubtUndSpeichertVermerk() {
        long personId = mitgliedMit(LocalDate.now().minusDays(1)); // abgelaufen

        Besuch besuch = service.anmelden(personId, List.of(skeetId), "Ausweis gesehen, Override");

        assertThat(besuch.status()).isEqualTo(BesuchStatus.ANGEMELDET);
        assertThat(besuch.lizenzVermerk()).isEqualTo("Ausweis gesehen, Override");
    }

    @Test
    void standZuteilungAendernErsetztProvisorischeBuchungen() {
        long personId = mitgliedMit(LocalDate.now().plusYears(1));
        Besuch besuch = service.anmelden(personId, List.of(skeetId), null);

        service.standZuteilungAendern(besuch.id(), List.of(trapId, kugelId));

        assertThat(standbuchungRepository.findeByBesuch(besuch.id()))
                .extracting(Standbuchung::standId)
                .containsExactlyInAnyOrder(trapId, kugelId);
    }
}

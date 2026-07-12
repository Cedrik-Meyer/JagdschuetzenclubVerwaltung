package de.jsc.kasse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.lizenz.LizenzPruefer;
import de.jsc.kasse.persistence.BesuchRepository;
import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.StandbuchungRepository;
import de.jsc.kasse.persistence.jdbc.BesuchRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.SchiesserlaubnisRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandbuchungRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.TarifRepositoryJdbc;
import de.jsc.kasse.pricing.PreisRechner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KassenServiceTest {

    private Datenbank db;
    private PersonRepositoryJdbc personRepository;
    private SchiesserlaubnisRepositoryJdbc schiesserlaubnisRepository;
    private BesuchRepository besuchRepository;
    private StandbuchungRepository standbuchungRepository;
    private StandRepositoryJdbc standRepository;
    private StammdatenService stammdatenService;
    private AnmeldeService anmeldeService;
    private KassenService service;

    private long skeetId;
    private long trapId;
    private long kugelId;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        personRepository = new PersonRepositoryJdbc(db.verbindung());
        schiesserlaubnisRepository = new SchiesserlaubnisRepositoryJdbc(db.verbindung());
        besuchRepository = new BesuchRepositoryJdbc(db.verbindung());
        standbuchungRepository = new StandbuchungRepositoryJdbc(db.verbindung());
        standRepository = new StandRepositoryJdbc(db.verbindung());
        TarifRepositoryJdbc tarifRepository = new TarifRepositoryJdbc(db.verbindung());

        stammdatenService = new StammdatenService(standRepository, tarifRepository);
        anmeldeService = new AnmeldeService(besuchRepository, standbuchungRepository,
                standRepository, schiesserlaubnisRepository, new LizenzPruefer());
        service = new KassenService(besuchRepository, standbuchungRepository, standRepository,
                personRepository, stammdatenService, new PreisRechner());

        skeetId = standRepository.findeByName("Skeet").orElseThrow().id();
        trapId = standRepository.findeByName("Trap").orElseThrow().id();
        kugelId = standRepository.findeByName("Kugel").orElseThrow().id();

        LocalDate frueher = LocalDate.now().minusYears(1);
        stammdatenService.neuenTarifAnlegen(skeetId, 12, 15, frueher);
        stammdatenService.neuenTarifAnlegen(trapId, 10, 13, frueher);
        stammdatenService.neuenTarifAnlegen(kugelId, 500, 700, frueher);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private long person(PersonTyp typ) {
        Person p = personRepository.anlegen(new Person(0, typ, "Vor", "Nach", null,
                typ == PersonTyp.MITGLIED ? "M-" + System.nanoTime() : null, null,
                true, null, null, LocalDateTime.now()));
        schiesserlaubnisRepository.anlegen(new Schiesserlaubnis(0, p.id(), LizenzArt.JAGDSCHEIN,
                "JS", LocalDate.now().plusYears(1), null));
        return p.id();
    }

    private long anmelden(long personId, List<Long> standIds) {
        return anmeldeService.anmelden(personId, standIds, null).id();
    }

    @Test
    void happyPathMitgliedRechnetKorrektUndFriertSnapshotsEin() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(skeetId, trapId, kugelId));

        Besuch abgerechnet = service.abrechnenUndAbmelden(besuchId, List.of(
                new PositionsAngabe(skeetId, 25),   // 25*12 = 300
                new PositionsAngabe(trapId, 50),    // 50*10 = 500
                new PositionsAngabe(kugelId, 3)));  // pauschal 500

        assertThat(abgerechnet.gesamtCent()).isEqualTo(1300);
        assertThat(abgerechnet.status()).isEqualTo(BesuchStatus.ABGEMELDET);
        assertThat(abgerechnet.bezahlt()).isTrue();
        assertThat(abgerechnet.checkOut()).isNotNull();
        assertThat(abgerechnet.bezahltAm()).isNotNull();

        List<Standbuchung> zeilen = standbuchungRepository.findeByBesuch(besuchId);
        assertThat(zeilen).hasSize(3);
        assertThat(zeilen).filteredOn(z -> z.standId() == skeetId).singleElement()
                .satisfies(z -> {
                    assertThat(z.menge()).isEqualTo(25);
                    assertThat(z.einzelpreisCent()).isEqualTo(12);
                    assertThat(z.zeilenpreisCent()).isEqualTo(300);
                });
        assertThat(zeilen).filteredOn(z -> z.standId() == kugelId).singleElement()
                .satisfies(z -> {
                    assertThat(z.menge()).isEqualTo(1);
                    assertThat(z.zeilenpreisCent()).isEqualTo(500);
                });
    }

    @Test
    void gastZahltGastpreise() {
        long besuchId = anmelden(person(PersonTyp.GAST), List.of(skeetId, kugelId));

        Besuch abgerechnet = service.abrechnenUndAbmelden(besuchId, List.of(
                new PositionsAngabe(skeetId, 25),   // 25*15 = 375
                new PositionsAngabe(kugelId, 3)));  // pauschal 700

        assertThat(abgerechnet.gesamtCent()).isEqualTo(1075);
    }

    @Test
    void skeetMitNullTaubenWirdNichtBerechnet() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(skeetId, kugelId));

        Besuch abgerechnet = service.abrechnenUndAbmelden(besuchId, List.of(
                new PositionsAngabe(skeetId, 0),
                new PositionsAngabe(kugelId, 1)));

        assertThat(abgerechnet.gesamtCent()).isEqualTo(500);
        assertThat(standbuchungRepository.findeByBesuch(besuchId))
                .extracting(Standbuchung::standId).containsExactly(kugelId);
    }

    @Test
    void pauschalDreiDurchgaengeErgibtEinfachenTagespreisMengeEins() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(kugelId));

        Besuch abgerechnet = service.abrechnenUndAbmelden(besuchId,
                List.of(new PositionsAngabe(kugelId, 3)));

        assertThat(abgerechnet.gesamtCent()).isEqualTo(500);
        assertThat(standbuchungRepository.findeByBesuch(besuchId)).singleElement()
                .extracting(Standbuchung::menge).isEqualTo(1);
    }

    @Test
    void preisvorschauSpeichertNichts() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(skeetId));

        assertThat(service.preisvorschau(besuchId, List.of(new PositionsAngabe(skeetId, 25)))
                .gesamtCent()).isEqualTo(300);

        Besuch unveraendert = besuchRepository.findeById(besuchId).orElseThrow();
        assertThat(unveraendert.status()).isEqualTo(BesuchStatus.ANGEMELDET);
        assertThat(unveraendert.gesamtCent()).isZero();
        assertThat(standbuchungRepository.findeByBesuch(besuchId)).singleElement()
                .extracting(Standbuchung::menge).isEqualTo(0); // noch provisorisch
    }

    @Test
    void checkoutEinesNichtOffenenBesuchsWirftException() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(skeetId));
        service.abrechnenUndAbmelden(besuchId, List.of(new PositionsAngabe(skeetId, 10)));

        assertThatThrownBy(() -> service.abrechnenUndAbmelden(besuchId,
                List.of(new PositionsAngabe(skeetId, 10))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void snapshotBleibtNachSpaeteremTarifStabil() {
        long besuchId = anmelden(person(PersonTyp.MITGLIED), List.of(skeetId));
        long gesamtVorher = service.abrechnenUndAbmelden(besuchId,
                List.of(new PositionsAngabe(skeetId, 25))).gesamtCent();
        assertThat(gesamtVorher).isEqualTo(300);

        // Neuer, teurerer Tarif mit späterem gueltig_ab.
        stammdatenService.neuenTarifAnlegen(skeetId, 99, 99, LocalDate.now());

        assertThat(besuchRepository.findeById(besuchId)).get()
                .extracting(Besuch::gesamtCent).isEqualTo(300L);
        assertThat(standbuchungRepository.findeByBesuch(besuchId)).singleElement()
                .extracting(Standbuchung::einzelpreisCent).isEqualTo(12L);
    }
}

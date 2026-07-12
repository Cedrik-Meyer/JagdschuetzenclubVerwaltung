package de.jsc.kasse.service;

import de.jsc.kasse.domain.Abrechnungsart;
import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.persistence.BesuchRepository;
import de.jsc.kasse.persistence.PersonRepository;
import de.jsc.kasse.persistence.StandRepository;
import de.jsc.kasse.persistence.StandbuchungRepository;
import de.jsc.kasse.pricing.Preisaufstellung;
import de.jsc.kasse.pricing.Preiszeile;
import de.jsc.kasse.pricing.PositionsWunsch;
import de.jsc.kasse.pricing.PreisRechner;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Check-out an der Kasse (Phase 2): Hier werden die Positionen bepreist, die Snapshots
 * eingefroren und der Besuch abgeschlossen. Löst die gültigen Tarife zum {@code besuch.datum}
 * auf und ruft den reinen {@link PreisRechner}.
 *
 * <p>Kein UI-, kein JavaFX-Bezug.
 */
public final class KassenService {

    private final BesuchRepository besuchRepository;
    private final StandbuchungRepository standbuchungRepository;
    private final StandRepository standRepository;
    private final PersonRepository personRepository;
    private final StammdatenService stammdatenService;
    private final PreisRechner preisRechner;

    public KassenService(BesuchRepository besuchRepository,
                         StandbuchungRepository standbuchungRepository,
                         StandRepository standRepository,
                         PersonRepository personRepository,
                         StammdatenService stammdatenService,
                         PreisRechner preisRechner) {
        this.besuchRepository = besuchRepository;
        this.standbuchungRepository = standbuchungRepository;
        this.standRepository = standRepository;
        this.personRepository = personRepository;
        this.stammdatenService = stammdatenService;
        this.preisRechner = preisRechner;
    }

    /**
     * Unverbindliche Preisvorschau für einen Besuch; speichert nichts.
     *
     * @throws IllegalArgumentException wenn Besuch, Person oder ein Stand unbekannt ist
     * @throws IllegalStateException    wenn für einen Stand kein Tarif zum Besuchsdatum gilt
     */
    public Preisaufstellung preisvorschau(long besuchId, List<PositionsAngabe> positionen) {
        Besuch besuch = ladeBesuch(besuchId);
        return preisRechner.berechne(istMitglied(besuch), baueWuensche(besuch, positionen));
    }

    /**
     * Rechnet den Besuch ab, ersetzt die provisorischen Buchungen durch die finalen bepreisten
     * Zeilen (Snapshot) und meldet die Person ab (bezahlt).
     *
     * @throws IllegalStateException    wenn der Besuch nicht (mehr) offen ist oder kein Tarif gilt
     * @throws IllegalArgumentException wenn Besuch, Person oder ein Stand unbekannt ist
     */
    public Besuch abrechnenUndAbmelden(long besuchId, List<PositionsAngabe> positionen) {
        Besuch besuch = ladeBesuch(besuchId);
        if (besuch.status() != BesuchStatus.ANGEMELDET) {
            throw new IllegalStateException("Besuch ist nicht offen: " + besuchId);
        }

        Preisaufstellung aufstellung =
                preisRechner.berechne(istMitglied(besuch), baueWuensche(besuch, positionen));

        standbuchungRepository.loescheByBesuch(besuchId);
        for (Preiszeile zeile : aufstellung.zeilen()) {
            standbuchungRepository.anlegen(new Standbuchung(0, besuchId, zeile.standId(),
                    zeile.menge(), zeile.einzelpreisCent(), zeile.zeilenpreisCent()));
        }

        LocalDateTime jetzt = LocalDateTime.now();
        Besuch abgerechnet = new Besuch(besuch.id(), besuch.personId(), besuch.datum(),
                besuch.checkIn(), jetzt, BesuchStatus.ABGEMELDET, aufstellung.gesamtCent(),
                true, jetzt, besuch.lizenzVermerk());
        besuchRepository.aktualisieren(abgerechnet);
        return abgerechnet;
    }

    private List<PositionsWunsch> baueWuensche(Besuch besuch, List<PositionsAngabe> positionen) {
        List<PositionsWunsch> wuensche = new ArrayList<>(positionen.size());
        for (PositionsAngabe angabe : positionen) {
            Stand stand = standRepository.findeById(angabe.standId())
                    .orElseThrow(() -> new IllegalArgumentException("Unbekannter Stand: " + angabe.standId()));

            int menge;
            if (stand.abrechnungsart() == Abrechnungsart.PAUSCHAL) {
                menge = 1; // Tagespreis: Menge normalisieren
            } else {
                if (angabe.menge() <= 0) {
                    continue; // PRO_TAUBE ohne Tauben -> Position verwerfen
                }
                menge = angabe.menge();
            }

            Tarif tarif = stammdatenService.aktuellerTarif(stand.id(), besuch.datum());
            wuensche.add(new PositionsWunsch(stand.id(), stand.abrechnungsart(), menge,
                    tarif.preisMitgliedCent(), tarif.preisGastCent()));
        }
        return wuensche;
    }

    private Besuch ladeBesuch(long besuchId) {
        return besuchRepository.findeById(besuchId)
                .orElseThrow(() -> new IllegalArgumentException("Besuch nicht gefunden: " + besuchId));
    }

    private boolean istMitglied(Besuch besuch) {
        Person person = personRepository.findeById(besuch.personId())
                .orElseThrow(() -> new IllegalArgumentException("Person nicht gefunden: " + besuch.personId()));
        return person.typ() == PersonTyp.MITGLIED;
    }
}

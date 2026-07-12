package de.jsc.kasse.service;

import de.jsc.kasse.domain.Abrechnungsart;
import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.domain.LizenzBewertung;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.lizenz.LizenzPruefer;
import de.jsc.kasse.lizenz.LizenzStatus;
import de.jsc.kasse.persistence.BesuchRepository;
import de.jsc.kasse.persistence.SchiesserlaubnisRepository;
import de.jsc.kasse.persistence.StandRepository;
import de.jsc.kasse.persistence.StandbuchungRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Check-in am Tresen (Phase 1): Lizenzprüfung, Anlegen des offenen Besuchs und der
 * provisorischen Standzuteilung — <strong>ohne</strong> Preisberechnung. Die Bepreisung und
 * das Einfrieren der Snapshots erfolgen erst beim Check-out im {@link KassenService}.
 *
 * <p>Kein UI-, kein JavaFX-Bezug. Ruft den reinen {@link LizenzPruefer} nur auf.
 */
public final class AnmeldeService {

    private final BesuchRepository besuchRepository;
    private final StandbuchungRepository standbuchungRepository;
    private final StandRepository standRepository;
    private final SchiesserlaubnisRepository schiesserlaubnisRepository;
    private final LizenzPruefer lizenzPruefer;

    public AnmeldeService(BesuchRepository besuchRepository,
                          StandbuchungRepository standbuchungRepository,
                          StandRepository standRepository,
                          SchiesserlaubnisRepository schiesserlaubnisRepository,
                          LizenzPruefer lizenzPruefer) {
        this.besuchRepository = besuchRepository;
        this.standbuchungRepository = standbuchungRepository;
        this.standRepository = standRepository;
        this.schiesserlaubnisRepository = schiesserlaubnisRepository;
        this.lizenzPruefer = lizenzPruefer;
    }

    /** Bewertet die Erlaubnisse der Person zum Stichtag (jede gültige genügt). */
    public LizenzStatus lizenzPruefen(long personId, LocalDate stichtag) {
        List<Schiesserlaubnis> lizenzen = schiesserlaubnisRepository.findeByPerson(personId);
        return lizenzPruefer.pruefe(lizenzen, null, stichtag);
    }

    /**
     * Meldet eine Person an und legt provisorische Standbuchungen an.
     *
     * @param lizenzVermerk Override-Begründung; nötig, wenn die Lizenz nicht gültig ist
     * @throws IllegalStateException    wenn bereits ein offener Besuch besteht oder die Lizenz
     *                                  ungültig ist und kein Vermerk gesetzt wurde
     * @throws IllegalArgumentException wenn eine Stand-ID unbekannt ist
     */
    public Besuch anmelden(long personId, List<Long> standIds, String lizenzVermerk) {
        boolean bereitsOffen = besuchRepository.findeByPerson(personId).stream()
                .anyMatch(b -> b.status() == BesuchStatus.ANGEMELDET);
        if (bereitsOffen) {
            throw new IllegalStateException("Person hat bereits einen offenen Besuch: " + personId);
        }

        LocalDate heute = LocalDate.now();
        LizenzStatus status = lizenzPruefen(personId, heute);
        boolean vermerkGesetzt = lizenzVermerk != null && !lizenzVermerk.isBlank();
        if (status.bewertung() != LizenzBewertung.GUELTIG && !vermerkGesetzt) {
            throw new IllegalStateException("Lizenz nicht gültig (" + status.bewertung()
                    + "): neues Ablaufdatum eintragen oder Override-Vermerk angeben.");
        }

        Besuch besuch = besuchRepository.anlegen(new Besuch(0, personId, heute,
                LocalDateTime.now(), null, BesuchStatus.ANGEMELDET, 0, false, null,
                vermerkGesetzt ? lizenzVermerk : null));

        legeProvisorischeBuchungenAn(besuch.id(), standIds);
        return besuch;
    }

    /**
     * Ersetzt die provisorische Standzuteilung eines offenen Besuchs.
     *
     * @throws IllegalArgumentException wenn der Besuch oder eine Stand-ID unbekannt ist
     * @throws IllegalStateException    wenn der Besuch nicht (mehr) offen ist
     */
    public void standZuteilungAendern(long besuchId, List<Long> standIds) {
        Besuch besuch = besuchRepository.findeById(besuchId)
                .orElseThrow(() -> new IllegalArgumentException("Besuch nicht gefunden: " + besuchId));
        if (besuch.status() != BesuchStatus.ANGEMELDET) {
            throw new IllegalStateException("Standzuteilung nur bei offenem Besuch änderbar: " + besuchId);
        }
        standbuchungRepository.loescheByBesuch(besuchId);
        legeProvisorischeBuchungenAn(besuchId, standIds);
    }

    /** Alle offenen Besuche (Status ANGEMELDET) für die Tresen-Übersicht. */
    public List<Besuch> offeneBesuche() {
        return besuchRepository.findeByStatus(BesuchStatus.ANGEMELDET);
    }

    private void legeProvisorischeBuchungenAn(long besuchId, List<Long> standIds) {
        for (long standId : standIds) {
            Stand stand = standRepository.findeById(standId)
                    .orElseThrow(() -> new IllegalArgumentException("Unbekannter Stand: " + standId));
            int menge = stand.abrechnungsart() == Abrechnungsart.PAUSCHAL ? 1 : 0;
            standbuchungRepository.anlegen(new Standbuchung(0, besuchId, standId, menge, 0, 0));
        }
    }
}

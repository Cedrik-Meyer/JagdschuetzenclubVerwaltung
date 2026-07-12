package de.jsc.kasse.service;

import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.persistence.StandRepository;
import de.jsc.kasse.persistence.TarifRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Anwendungsfälle rund um Stammdaten: Stände und ihre Tarife (mit Preishistorie).
 *
 * <p>Orchestriert die Repositories; kein UI-, kein JavaFX-Bezug. Preise als {@code long} in
 * Cent. Diese Klasse löst nur den gültigen Tarif auf — die eigentliche Preisberechnung liegt
 * im reinen {@code pricing}-Modul.
 */
public final class StammdatenService {

    private final StandRepository standRepository;
    private final TarifRepository tarifRepository;

    public StammdatenService(StandRepository standRepository, TarifRepository tarifRepository) {
        this.standRepository = standRepository;
        this.tarifRepository = tarifRepository;
    }

    /** Alle Stände, optional nur die aktiven. */
    public List<Stand> alleStaende(boolean nurAktive) {
        return standRepository.findeAlle().stream()
                .filter(s -> !nurAktive || s.aktiv())
                .toList();
    }

    /**
     * Der zum {@code datum} gültige Tarif eines Standes: der mit dem spätesten
     * {@code gueltigAb}, das nicht nach {@code datum} liegt.
     *
     * @throws IllegalArgumentException wenn der Stand nicht existiert
     * @throws IllegalStateException    wenn zum {@code datum} kein Tarif gilt
     */
    public Tarif aktuellerTarif(long standId, LocalDate datum) {
        Stand stand = standRepository.findeById(standId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Stand: " + standId));
        return tarifRepository.findeByStand(standId).stream()
                .filter(t -> !t.gueltigAb().isAfter(datum))
                .max(Comparator.comparing(Tarif::gueltigAb).thenComparing(Tarif::id))
                .orElseThrow(() -> new IllegalStateException(
                        "Kein gültiger Tarif für Stand '" + stand.name() + "' zum " + datum));
    }

    /**
     * Legt einen neuen Tarif-Datensatz an (die bisherige Historie bleibt erhalten).
     *
     * @throws IllegalArgumentException wenn der Stand nicht existiert oder ein Preis negativ ist
     */
    public Tarif neuenTarifAnlegen(long standId, long preisMitgliedCent, long preisGastCent,
                                   LocalDate gueltigAb) {
        standRepository.findeById(standId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Stand: " + standId));
        if (preisMitgliedCent < 0 || preisGastCent < 0) {
            throw new IllegalArgumentException("Preise dürfen nicht negativ sein");
        }
        return tarifRepository.anlegen(new Tarif(0, standId, preisMitgliedCent, preisGastCent, gueltigAb));
    }

    /** Tarifhistorie eines Standes, absteigend nach {@code gueltigAb}. */
    public List<Tarif> tarifHistorie(long standId) {
        return tarifRepository.findeByStand(standId).stream()
                .sorted(Comparator.comparing(Tarif::gueltigAb).thenComparing(Tarif::id).reversed())
                .toList();
    }

    /**
     * Aktiviert oder deaktiviert einen Stand.
     *
     * @throws IllegalArgumentException wenn der Stand nicht existiert
     */
    public void standAktivSetzen(long standId, boolean aktiv) {
        Stand stand = standRepository.findeById(standId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Stand: " + standId));
        standRepository.aktualisieren(
                new Stand(stand.id(), stand.name(), stand.abrechnungsart(),
                        stand.erforderlicheLizenz(), aktiv));
    }
}

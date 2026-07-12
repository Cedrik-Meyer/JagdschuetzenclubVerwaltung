package de.jsc.kasse.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ein Besuch (Check-in / Check-out) einer Person. Felder nach ARCHITEKTUR.md 4.3.
 *
 * <p>Wird niemals hart gelöscht (Aufbewahrungspflicht). {@code gesamtCent} ist ein
 * eingefrorener Snapshot (long, Cent). {@code id == 0} bedeutet „noch nicht persistiert".
 * Nullbar: {@code checkOut}, {@code bezahltAm}, {@code lizenzVermerk}.
 *
 * <p>{@code lizenzVermerk} (Schema v2): {@code null} = kein Override; Text = Anmeldung trotz
 * ungültiger Lizenz mit Begründung.
 */
public record Besuch(
        long id,
        long personId,
        LocalDate datum,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        BesuchStatus status,
        long gesamtCent,
        boolean bezahlt,
        LocalDateTime bezahltAm,
        String lizenzVermerk
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Besuch mitId(long neueId) {
        return new Besuch(neueId, personId, datum, checkIn, checkOut, status,
                gesamtCent, bezahlt, bezahltAm, lizenzVermerk);
    }
}

package de.jsc.kasse.domain;

import java.time.LocalDate;

/**
 * Tarif eines Standes (getrennte Mitglieds-/Gastpreise). Felder nach ARCHITEKTUR.md 4.3.
 *
 * <p>Preise als {@code long} in Cent. {@code gueltigAb} bildet die Preishistorie.
 * {@code id == 0} bedeutet „noch nicht persistiert".
 */
public record Tarif(
        long id,
        long standId,
        long preisMitgliedCent,
        long preisGastCent,
        LocalDate gueltigAb
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Tarif mitId(long neueId) {
        return new Tarif(neueId, standId, preisMitgliedCent, preisGastCent, gueltigAb);
    }
}

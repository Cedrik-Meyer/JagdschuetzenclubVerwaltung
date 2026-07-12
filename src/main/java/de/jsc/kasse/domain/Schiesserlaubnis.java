package de.jsc.kasse.domain;

import java.time.LocalDate;

/**
 * Schießerlaubnis einer Person. Felder nach ARCHITEKTUR.md 4.3.
 *
 * <p>{@code id == 0} bedeutet „noch nicht persistiert". Nullbar: {@code nummer},
 * {@code ausgestelltAm}.
 */
public record Schiesserlaubnis(
        long id,
        long personId,
        LizenzArt art,
        String nummer,
        LocalDate ablaufdatum,
        LocalDate ausgestelltAm
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Schiesserlaubnis mitId(long neueId) {
        return new Schiesserlaubnis(neueId, personId, art, nummer, ablaufdatum, ausgestelltAm);
    }
}

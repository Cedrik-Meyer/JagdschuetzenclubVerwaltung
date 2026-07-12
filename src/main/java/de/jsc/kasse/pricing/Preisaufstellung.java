package de.jsc.kasse.pricing;

import java.util.List;

/**
 * Ergebnis der Preisberechnung: die Belegzeilen in Eingabereihenfolge und die Gesamtsumme
 * (in Cent).
 */
public record Preisaufstellung(
        List<Preiszeile> zeilen,
        long gesamtCent
) {
    /** Sichert die Zeilenliste gegen nachträgliche Veränderung ab. */
    public Preisaufstellung {
        zeilen = List.copyOf(zeilen);
    }
}

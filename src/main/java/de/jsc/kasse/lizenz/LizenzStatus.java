package de.jsc.kasse.lizenz;

import de.jsc.kasse.domain.LizenzBewertung;
import java.time.LocalDate;

/**
 * Ergebnis einer Lizenzprüfung (ARCHITEKTUR.md 5.2).
 *
 * @param bewertung     GUELTIG, ABGELAUFEN oder FEHLT
 * @param ablaufdatum   maßgebliches Ablaufdatum ({@code null} bei FEHLT)
 * @param tageBisAblauf Tage von Stichtag bis Ablaufdatum (negativ wenn abgelaufen, 0 bei FEHLT)
 * @param warnung       Vorwarnung „läuft im laufenden Jahr ab" (nur bei GUELTIG möglich)
 */
public record LizenzStatus(
        LizenzBewertung bewertung,
        LocalDate ablaufdatum,
        long tageBisAblauf,
        boolean warnung
) {
}

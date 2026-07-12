package de.jsc.kasse.lizenz;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.LizenzBewertung;
import de.jsc.kasse.domain.Schiesserlaubnis;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Reiner Lizenz-Prüfer (keine DB, keine UI). Bewertet, ob zum Stichtag eine gültige
 * Erlaubnis der geforderten Art vorliegt (ARCHITEKTUR.md 5.2).
 *
 * <p>{@code erforderlich == null} bedeutet: jede Erlaubnis ist relevant. Eine Erlaubnis gilt
 * als gültig, solange {@code stichtag <= ablaufdatum} (der Ablauftag zählt noch als gültig).
 */
public final class LizenzPruefer {

    /**
     * @param lizenzen     die vorliegenden Erlaubnisse der Person
     * @param erforderlich geforderte Lizenzart, oder {@code null} für „beliebige gültige"
     * @param stichtag     Prüfdatum
     * @return Bewertung, maßgebliches Ablaufdatum, Tage bis Ablauf und Vorwarnung
     */
    public LizenzStatus pruefe(List<Schiesserlaubnis> lizenzen, LizenzArt erforderlich,
                               LocalDate stichtag) {
        List<Schiesserlaubnis> relevante = lizenzen.stream()
                .filter(l -> erforderlich == null || l.art() == erforderlich)
                .toList();

        if (relevante.isEmpty()) {
            return new LizenzStatus(LizenzBewertung.FEHLT, null, 0, false);
        }

        List<Schiesserlaubnis> gueltige = relevante.stream()
                .filter(l -> !stichtag.isAfter(l.ablaufdatum()))
                .toList();

        if (gueltige.isEmpty()) {
            LocalDate ablauf = spaetestesAblaufdatum(relevante);
            return new LizenzStatus(LizenzBewertung.ABGELAUFEN, ablauf,
                    ChronoUnit.DAYS.between(stichtag, ablauf), false);
        }

        LocalDate ablauf = spaetestesAblaufdatum(gueltige);
        boolean warnung = ablauf.getYear() == stichtag.getYear();
        return new LizenzStatus(LizenzBewertung.GUELTIG, ablauf,
                ChronoUnit.DAYS.between(stichtag, ablauf), warnung);
    }

    private static LocalDate spaetestesAblaufdatum(List<Schiesserlaubnis> erlaubnisse) {
        return erlaubnisse.stream()
                .map(Schiesserlaubnis::ablaufdatum)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }
}

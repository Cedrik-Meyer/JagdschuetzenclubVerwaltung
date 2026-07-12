package de.jsc.kasse.pricing;

import java.util.ArrayList;
import java.util.List;

/**
 * Reine Preis-Engine (keine DB, keine UI). Berechnet aus den gewünschten Positionen und
 * bereits aufgelösten Tarifen eine {@link Preisaufstellung}.
 *
 * <p>Regeln (ARCHITEKTUR.md 5.1): {@code PRO_TAUBE} rechnet mengenabhängig, {@code PAUSCHAL}
 * ist ein Tagespreis je Stand (Menge fließt nicht ein, Belegzeile mit {@code menge == 1}).
 * Ganzzahlige Cent, keine Rundung.
 */
public final class PreisRechner {

    /**
     * @param istMitglied ob der Mitgliedspreis gilt (sonst Gastpreis)
     * @param positionen  die gewünschten Positionen in gewünschter Reihenfolge
     * @return Belegzeilen (in Eingabereihenfolge) und Gesamtsumme
     * @throws IllegalArgumentException bei {@code menge < 1} oder negativen Preisen
     */
    public Preisaufstellung berechne(boolean istMitglied, List<PositionsWunsch> positionen) {
        List<Preiszeile> zeilen = new ArrayList<>(positionen.size());
        long gesamtCent = 0;

        for (PositionsWunsch position : positionen) {
            pruefe(position);
            long einzelpreisCent = istMitglied ? position.preisMitgliedCent() : position.preisGastCent();

            Preiszeile zeile = switch (position.art()) {
                case PRO_TAUBE -> new Preiszeile(position.standId(), position.menge(),
                        einzelpreisCent, einzelpreisCent * position.menge());
                case PAUSCHAL -> new Preiszeile(position.standId(), 1,
                        einzelpreisCent, einzelpreisCent);
            };

            zeilen.add(zeile);
            gesamtCent += zeile.zeilenpreisCent();
        }

        return new Preisaufstellung(zeilen, gesamtCent);
    }

    private static void pruefe(PositionsWunsch position) {
        if (position.menge() < 1) {
            throw new IllegalArgumentException("menge muss >= 1 sein, war: " + position.menge());
        }
        if (position.preisMitgliedCent() < 0) {
            throw new IllegalArgumentException(
                    "preisMitgliedCent muss >= 0 sein, war: " + position.preisMitgliedCent());
        }
        if (position.preisGastCent() < 0) {
            throw new IllegalArgumentException(
                    "preisGastCent muss >= 0 sein, war: " + position.preisGastCent());
        }
    }
}

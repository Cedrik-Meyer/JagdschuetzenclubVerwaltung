package de.jsc.kasse.pricing;

import de.jsc.kasse.domain.Abrechnungsart;

/**
 * Eine gewünschte Position der Preisberechnung. Enthält bereits die aufgelösten Tarife
 * (Mitglieds- und Gastpreis in Cent), damit die Engine ohne DB auskommt.
 *
 * <p>{@code menge} = Tauben (PRO_TAUBE) bzw. Durchgänge (PAUSCHAL).
 */
public record PositionsWunsch(
        long standId,
        Abrechnungsart art,
        int menge,
        long preisMitgliedCent,
        long preisGastCent
) {
}

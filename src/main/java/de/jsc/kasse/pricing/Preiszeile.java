package de.jsc.kasse.pricing;

/**
 * Eine berechnete Belegzeile. {@code einzelpreisCent} und {@code zeilenpreisCent} sind die
 * Werte, die bei der Buchung als Snapshot eingefroren werden.
 *
 * <p>Bei PAUSCHAL ist {@code menge == 1} (Tagespreis, saubere Belegzeile).
 */
public record Preiszeile(
        long standId,
        int menge,
        long einzelpreisCent,
        long zeilenpreisCent
) {
}

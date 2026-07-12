package de.jsc.kasse.domain;

/**
 * Eine Buchungszeile eines Besuchs an einem Stand. Felder nach ARCHITEKTUR.md 4.3.
 *
 * <p>{@code einzelpreisCent} und {@code zeilenpreisCent} sind eingefrorene Snapshots
 * (long, Cent) — spätere Tarifänderungen verändern historische Belege nicht.
 * {@code menge} = Tauben (PRO_TAUBE) bzw. Durchgänge (PAUSCHAL).
 * {@code id == 0} bedeutet „noch nicht persistiert".
 */
public record Standbuchung(
        long id,
        long besuchId,
        long standId,
        int menge,
        long einzelpreisCent,
        long zeilenpreisCent
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Standbuchung mitId(long neueId) {
        return new Standbuchung(neueId, besuchId, standId, menge, einzelpreisCent, zeilenpreisCent);
    }
}

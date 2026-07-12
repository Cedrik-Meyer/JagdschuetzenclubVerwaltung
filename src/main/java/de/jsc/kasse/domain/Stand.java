package de.jsc.kasse.domain;

/**
 * Schießstand. Felder nach ARCHITEKTUR.md 4.3.
 *
 * <p>{@code id == 0} bedeutet „noch nicht persistiert". {@code erforderlicheLizenz == null}
 * bedeutet „jede gültige Erlaubnis genügt".
 */
public record Stand(
        long id,
        String name,
        Abrechnungsart abrechnungsart,
        LizenzArt erforderlicheLizenz,
        boolean aktiv
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Stand mitId(long neueId) {
        return new Stand(neueId, name, abrechnungsart, erforderlicheLizenz, aktiv);
    }
}

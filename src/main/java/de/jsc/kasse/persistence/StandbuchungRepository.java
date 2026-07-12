package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Standbuchung;
import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf {@link Standbuchung}. Buchungszeilen sind eingefrorene Belege
 * (Snapshot-Preise) und werden nicht verändert oder gelöscht.
 */
public interface StandbuchungRepository {

    /** Legt eine neue Buchungszeile an und liefert sie mit vergebener ID zurück. */
    Standbuchung anlegen(Standbuchung standbuchung);

    Optional<Standbuchung> findeById(long id);

    List<Standbuchung> findeByBesuch(long besuchId);
}

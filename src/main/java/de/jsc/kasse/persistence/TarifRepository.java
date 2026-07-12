package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Tarif;
import java.util.List;
import java.util.Optional;

/** Zugriff auf {@link Tarif}. Tarife werden zur Preishistorie behalten (kein Löschen). */
public interface TarifRepository {

    /** Legt einen neuen Tarif an und liefert ihn mit vergebener ID zurück. */
    Tarif anlegen(Tarif tarif);

    Optional<Tarif> findeById(long id);

    /** Alle Tarife eines Standes (Historie), aufsteigend nach {@code gueltigAb}. */
    List<Tarif> findeByStand(long standId);
}

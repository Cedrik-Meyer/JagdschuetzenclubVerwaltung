package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Stand;
import java.util.List;
import java.util.Optional;

/** Zugriff auf {@link Stand}. Die sechs Standard-Stände werden beim Erststart geseedet. */
public interface StandRepository {

    /** Legt einen neuen Stand an und liefert ihn mit vergebener ID zurück. */
    Stand anlegen(Stand stand);

    /** Aktualisiert einen bestehenden Stand (inkl. Deaktivierung). */
    void aktualisieren(Stand stand);

    Optional<Stand> findeById(long id);

    Optional<Stand> findeByName(String name);

    List<Stand> findeAlle();
}

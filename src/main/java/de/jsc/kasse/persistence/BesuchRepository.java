package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Besuch;
import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf {@link Besuch}. Besuche werden nie hart gelöscht (Aufbewahrungspflicht);
 * Statuswechsel/Bezahlung erfolgen über {@link #aktualisieren(Besuch)}.
 */
public interface BesuchRepository {

    /** Legt einen neuen Besuch an und liefert ihn mit vergebener ID zurück. */
    Besuch anlegen(Besuch besuch);

    /** Aktualisiert einen bestehenden Besuch (Check-out, Gesamtbetrag, Bezahlung). */
    void aktualisieren(Besuch besuch);

    Optional<Besuch> findeById(long id);

    List<Besuch> findeByPerson(long personId);
}

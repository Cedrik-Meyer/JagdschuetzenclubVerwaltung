package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Schiesserlaubnis;
import java.util.List;
import java.util.Optional;

/** Zugriff auf {@link Schiesserlaubnis}. */
public interface SchiesserlaubnisRepository {

    /** Legt eine neue Erlaubnis an und liefert sie mit vergebener ID zurück. */
    Schiesserlaubnis anlegen(Schiesserlaubnis erlaubnis);

    /** Aktualisiert eine bestehende Erlaubnis (z. B. neues Ablaufdatum). */
    void aktualisieren(Schiesserlaubnis erlaubnis);

    Optional<Schiesserlaubnis> findeById(long id);

    List<Schiesserlaubnis> findeByPerson(long personId);
}

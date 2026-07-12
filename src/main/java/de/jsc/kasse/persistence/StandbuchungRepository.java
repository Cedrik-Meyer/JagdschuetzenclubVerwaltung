package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Standbuchung;
import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf {@link Standbuchung}. Die finalen (bepreisten) Buchungszeilen eines
 * abgemeldeten Besuchs sind eingefrorene Belege und werden nicht mehr verändert.
 *
 * <p>{@link #loescheByBesuch(long)} dient ausschließlich dem Ersetzen der
 * <em>provisorischen</em> Zeilen eines noch offenen Besuchs (Menge/Preis = 0) bei
 * Standzuteilungs-Änderung bzw. beim Check-out, bevor die endgültigen Zeilen geschrieben
 * werden — es betrifft keine abgeschlossenen Belege.
 */
public interface StandbuchungRepository {

    /** Legt eine neue Buchungszeile an und liefert sie mit vergebener ID zurück. */
    Standbuchung anlegen(Standbuchung standbuchung);

    Optional<Standbuchung> findeById(long id);

    List<Standbuchung> findeByBesuch(long besuchId);

    /** Entfernt alle (provisorischen) Buchungszeilen eines offenen Besuchs. */
    void loescheByBesuch(long besuchId);
}

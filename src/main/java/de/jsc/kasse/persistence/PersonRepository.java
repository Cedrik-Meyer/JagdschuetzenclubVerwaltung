package de.jsc.kasse.persistence;

import de.jsc.kasse.domain.Person;
import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf {@link Person}. Personen werden nie hart gelöscht — Deaktivierung
 * erfolgt über {@code aktiv} via {@link #aktualisieren(Person)}.
 */
public interface PersonRepository {

    /** Legt eine neue Person an und liefert sie mit vergebener ID zurück. */
    Person anlegen(Person person);

    /** Aktualisiert eine bestehende Person (inkl. Deaktivierung). */
    void aktualisieren(Person person);

    Optional<Person> findeById(long id);

    List<Person> findeAlle();
}

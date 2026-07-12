package de.jsc.kasse.persistence;

/**
 * Unchecked-Wrapper für Fehler der Persistenzschicht (z. B. {@link java.sql.SQLException}),
 * damit die Repository-Signaturen frei von checked Exceptions bleiben.
 */
public class PersistenzException extends RuntimeException {

    public PersistenzException(String nachricht, Throwable ursache) {
        super(nachricht, ursache);
    }

    public PersistenzException(String nachricht) {
        super(nachricht);
    }
}

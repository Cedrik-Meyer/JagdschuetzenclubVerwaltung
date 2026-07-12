package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prüft, dass die CHECK- und Fremdschlüssel-Constraints des Schemas greifen — über
 * rohes SQL, das die typsichere Domänenschicht bewusst umgeht.
 */
class ConstraintTest {

    private Datenbank db;
    private Connection verbindung;

    @BeforeEach
    void setUp() {
        db = Datenbank.imArbeitsspeicher();
        verbindung = db.verbindung();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void ungueltigerPersonTypVerletztCheckConstraint() throws SQLException {
        try (PreparedStatement ps = verbindung.prepareStatement(
                "INSERT INTO person(typ, vorname, nachname, aktiv, angelegt_am) "
                + "VALUES('UNBEKANNT', 'X', 'Y', 1, ?)")) {
            ps.setString(1, LocalDateTime.now().toString());
            assertThatThrownBy(ps::executeUpdate).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void ungueltigeAbrechnungsartVerletztCheckConstraint() throws SQLException {
        try (PreparedStatement ps = verbindung.prepareStatement(
                "INSERT INTO stand(name, abrechnungsart, aktiv) VALUES('Test', 'STUECK', 1)")) {
            assertThatThrownBy(ps::executeUpdate).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void fremdschluesselAufFehlendenBesuchVerletztConstraint() throws SQLException {
        try (PreparedStatement ps = verbindung.prepareStatement(
                "INSERT INTO standbuchung(besuch_id, stand_id, menge, einzelpreis_cent, "
                + "zeilenpreis_cent) VALUES(999999, 1, 1, 100, 100)")) {
            assertThatThrownBy(ps::executeUpdate).isInstanceOf(SQLException.class);
        }
    }
}

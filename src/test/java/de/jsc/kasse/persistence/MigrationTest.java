package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prüft die versionierte Schema-Migration v1 → v2 (Spalte {@code besuch.lizenz_vermerk}).
 */
class MigrationTest {

    @Test
    void hebtV1AufV2UndErgaenztLizenzVermerk(@TempDir Path verzeichnis) throws Exception {
        Path datei = verzeichnis.resolve("alt.db");

        // v1-Datenbank von Hand aufsetzen: besuch OHNE lizenz_vermerk, schema_version = 1.
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + datei.toAbsolutePath());
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE besuch ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, "
                    + "datum TEXT NOT NULL, check_in TEXT NOT NULL, check_out TEXT, "
                    + "status TEXT NOT NULL, gesamt_cent INTEGER NOT NULL DEFAULT 0, "
                    + "bezahlt INTEGER NOT NULL DEFAULT 0, bezahlt_am TEXT)");
            st.execute("CREATE TABLE schema_version (version INTEGER NOT NULL)");
            st.execute("INSERT INTO schema_version(version) VALUES(1)");
        }

        // Öffnen über Datenbank -> Migration auf v2.
        try (Datenbank db = Datenbank.ausDatei(datei)) {
            assertThat(spalteVorhanden(db.verbindung(), "besuch", "lizenz_vermerk")).isTrue();

            try (Statement st = db.verbindung().createStatement();
                 ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    private static boolean spalteVorhanden(Connection c, String tabelle, String spalte)
            throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabelle + ")")) {
            while (rs.next()) {
                if (spalte.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}

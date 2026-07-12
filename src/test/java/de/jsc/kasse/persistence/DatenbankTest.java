package de.jsc.kasse.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.jsc.kasse.domain.Abrechnungsart;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatenbankTest {

    @Test
    void seedetSechsStaendeMitKorrekterAbrechnungsart() {
        try (Datenbank db = Datenbank.imArbeitsspeicher()) {
            List<Stand> staende = new StandRepositoryJdbc(db.verbindung()).findeAlle();

            assertThat(staende).extracting(Stand::name).containsExactly(
                    "Skeet", "Trap", "Kipphase", "Kugel", "Pistole", "Laufender Keiler");
            assertThat(staende).allMatch(Stand::aktiv);
            assertThat(staende).allMatch(s -> s.erforderlicheLizenz() == null);

            assertThat(staende).filteredOn(s -> s.name().equals("Skeet"))
                    .allMatch(s -> s.abrechnungsart() == Abrechnungsart.PRO_TAUBE);
            assertThat(staende).filteredOn(s -> s.name().equals("Kugel"))
                    .allMatch(s -> s.abrechnungsart() == Abrechnungsart.PAUSCHAL);
        }
    }

    @Test
    void setztSchemaVersion() throws Exception {
        try (Datenbank db = Datenbank.imArbeitsspeicher();
             Statement st = db.verbindung().createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(Datenbank.SCHEMA_VERSION);
        }
    }

    @Test
    void seedetBeimWiederOeffnenNichtErneut(@TempDir Path verzeichnis) {
        Path datei = verzeichnis.resolve("kasse.db");

        try (Datenbank db = Datenbank.ausDatei(datei)) {
            assertThat(new StandRepositoryJdbc(db.verbindung()).findeAlle()).hasSize(6);
        }
        // Zweites Öffnen derselben Datei darf nicht erneut seeden.
        try (Datenbank db = Datenbank.ausDatei(datei)) {
            assertThat(new StandRepositoryJdbc(db.verbindung()).findeAlle()).hasSize(6);
        }
    }
}

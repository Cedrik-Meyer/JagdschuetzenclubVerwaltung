package de.jsc.kasse.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Verbindungs- und Schema-Verwaltung für die SQLite-Datenbank.
 *
 * <p>Hält eine einzige, langlebige {@link Connection} (Ein-Rechner-, Ein-Bediener-Anwendung).
 * Beim Öffnen werden Fremdschlüssel aktiviert, das Schema aus {@code schema.sql} idempotent
 * ausgeführt und beim Erststart die sechs Stände geseedet. Die Versionierung erfolgt über die
 * Tabelle {@code schema_version}.
 *
 * <p>Für Tests liefert {@link #imArbeitsspeicher()} eine In-Memory-Datenbank
 * ({@code jdbc:sqlite::memory:}); da genau eine Verbindung gehalten wird, bleibt der Inhalt
 * über die Lebensdauer der Instanz erhalten.
 */
public final class Datenbank implements AutoCloseable {

    /** Aktuelle Schema-Version. */
    public static final int SCHEMA_VERSION = 2;

    private static final String SCHEMA_RESSOURCE = "/de/jsc/kasse/persistence/schema.sql";

    private final Connection verbindung;

    private Datenbank(String jdbcUrl) {
        try {
            this.verbindung = DriverManager.getConnection(jdbcUrl);
            aktiviereFremdschluessel();
            initialisiere();
        } catch (SQLException e) {
            throw new PersistenzException("Datenbank konnte nicht geöffnet werden: " + jdbcUrl, e);
        }
    }

    /** In-Memory-Datenbank (für Tests). */
    public static Datenbank imArbeitsspeicher() {
        return new Datenbank("jdbc:sqlite::memory:");
    }

    /** Datei-Datenbank unter dem angegebenen Pfad (wird bei Bedarf angelegt). */
    public static Datenbank ausDatei(Path pfad) {
        return new Datenbank("jdbc:sqlite:" + pfad.toAbsolutePath());
    }

    /** Die gehaltene Verbindung. Wird von den Repository-Implementierungen genutzt. */
    public Connection verbindung() {
        return verbindung;
    }

    private void aktiviereFremdschluessel() throws SQLException {
        try (Statement st = verbindung.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    private void initialisiere() throws SQLException {
        fuehreSchemaAus();
        int version = aktuelleVersion();
        if (version == 0) {
            // Frische DB: schema.sql enthält bereits den aktuellen Stand.
            seedeStaende();
            setzeVersion(SCHEMA_VERSION);
        } else if (version < SCHEMA_VERSION) {
            migriere(version);
        }
    }

    /** Hebt eine bestehende Datenbank schrittweise auf {@link #SCHEMA_VERSION}. */
    private void migriere(int vonVersion) throws SQLException {
        if (vonVersion < 2 && !spalteVorhanden("besuch", "lizenz_vermerk")) {
            try (Statement st = verbindung.createStatement()) {
                st.execute("ALTER TABLE besuch ADD COLUMN lizenz_vermerk TEXT");
            }
        }
        setzeVersion(SCHEMA_VERSION);
    }

    private boolean spalteVorhanden(String tabelle, String spalte) throws SQLException {
        try (Statement st = verbindung.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabelle + ")")) {
            while (rs.next()) {
                if (spalte.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void fuehreSchemaAus() throws SQLException {
        String script = entferneZeilenkommentare(ladeRessource(SCHEMA_RESSOURCE));
        for (String anweisung : script.split(";")) {
            String sql = anweisung.strip();
            if (!sql.isEmpty()) {
                try (Statement st = verbindung.createStatement()) {
                    st.execute(sql);
                }
            }
        }
    }

    /** Entfernt volle {@code --}-Kommentarzeilen, damit Semikolons in Kommentaren das
     *  Aufsplitten der Anweisungen nicht stören. */
    private static String entferneZeilenkommentare(String script) {
        StringBuilder sb = new StringBuilder();
        for (String zeile : script.split("\n")) {
            if (!zeile.strip().startsWith("--")) {
                sb.append(zeile).append('\n');
            }
        }
        return sb.toString();
    }

    private int aktuelleVersion() throws SQLException {
        try (Statement st = verbindung.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void setzeVersion(int version) throws SQLException {
        try (Statement st = verbindung.createStatement()) {
            st.executeUpdate("DELETE FROM schema_version");
            st.executeUpdate("INSERT INTO schema_version(version) VALUES(" + version + ")");
        }
    }

    private void seedeStaende() throws SQLException {
        record Seed(String name, String abrechnungsart) {}
        List<Seed> staende = List.of(
                new Seed("Skeet", "PRO_TAUBE"),
                new Seed("Trap", "PRO_TAUBE"),
                new Seed("Kipphase", "PAUSCHAL"),
                new Seed("Kugel", "PAUSCHAL"),
                new Seed("Pistole", "PAUSCHAL"),
                new Seed("Laufender Keiler", "PAUSCHAL"));

        String sql = "INSERT INTO stand(name, abrechnungsart, erforderliche_lizenz, aktiv) "
                + "VALUES(?, ?, NULL, 1)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            for (Seed s : staende) {
                ps.setString(1, s.name());
                ps.setString(2, s.abrechnungsart());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String ladeRessource(String pfad) {
        try (InputStream in = Datenbank.class.getResourceAsStream(pfad)) {
            if (in == null) {
                throw new PersistenzException("Ressource nicht gefunden: " + pfad);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenzException("Ressource nicht lesbar: " + pfad, e);
        }
    }

    @Override
    public void close() {
        try {
            verbindung.close();
        } catch (SQLException e) {
            throw new PersistenzException("Datenbank konnte nicht geschlossen werden", e);
        }
    }
}

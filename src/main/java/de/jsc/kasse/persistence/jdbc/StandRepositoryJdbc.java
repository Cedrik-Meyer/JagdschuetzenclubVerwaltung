package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.Abrechnungsart;
import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.persistence.PersistenzException;
import de.jsc.kasse.persistence.StandRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link StandRepository}. */
public final class StandRepositoryJdbc implements StandRepository {

    private static final String SPALTEN = "id, name, abrechnungsart, erforderliche_lizenz, aktiv";

    private final Connection verbindung;

    public StandRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Stand anlegen(Stand stand) {
        String sql = "INSERT INTO stand(name, abrechnungsart, erforderliche_lizenz, aktiv) "
                + "VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, stand);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return stand.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Stand konnte nicht angelegt werden", e);
        }
    }

    @Override
    public void aktualisieren(Stand stand) {
        String sql = "UPDATE stand SET name=?, abrechnungsart=?, erforderliche_lizenz=?, aktiv=? "
                + "WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            binde(ps, stand);
            ps.setLong(5, stand.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenzException("Stand konnte nicht aktualisiert werden", e);
        }
    }

    @Override
    public Optional<Stand> findeById(long id) {
        return findeEinzeln("SELECT " + SPALTEN + " FROM stand WHERE id=?", id);
    }

    @Override
    public Optional<Stand> findeByName(String name) {
        String sql = "SELECT " + SPALTEN + " FROM stand WHERE name=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Stand konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Stand> findeAlle() {
        String sql = "SELECT " + SPALTEN + " FROM stand ORDER BY id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Stand> ergebnis = new ArrayList<>();
            while (rs.next()) {
                ergebnis.add(lies(rs));
            }
            return ergebnis;
        } catch (SQLException e) {
            throw new PersistenzException("Stände konnten nicht gelesen werden", e);
        }
    }

    private Optional<Stand> findeEinzeln(String sql, long id) {
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Stand konnte nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Stand s) throws SQLException {
        ps.setString(1, s.name());
        ps.setString(2, s.abrechnungsart().name());
        Sql.setzeTextOderNull(ps, 3, s.erforderlicheLizenz() == null ? null : s.erforderlicheLizenz().name());
        ps.setInt(4, s.aktiv() ? 1 : 0);
    }

    private static Stand lies(ResultSet rs) throws SQLException {
        String lizenz = rs.getString("erforderliche_lizenz");
        return new Stand(
                rs.getLong("id"),
                rs.getString("name"),
                Abrechnungsart.valueOf(rs.getString("abrechnungsart")),
                lizenz == null ? null : LizenzArt.valueOf(lizenz),
                rs.getInt("aktiv") != 0);
    }
}

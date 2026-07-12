package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.Standbuchung;
import de.jsc.kasse.persistence.PersistenzException;
import de.jsc.kasse.persistence.StandbuchungRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link StandbuchungRepository}. */
public final class StandbuchungRepositoryJdbc implements StandbuchungRepository {

    private static final String SPALTEN =
            "id, besuch_id, stand_id, menge, einzelpreis_cent, zeilenpreis_cent";

    private final Connection verbindung;

    public StandbuchungRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Standbuchung anlegen(Standbuchung standbuchung) {
        String sql = "INSERT INTO standbuchung(besuch_id, stand_id, menge, einzelpreis_cent, "
                + "zeilenpreis_cent) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, standbuchung);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return standbuchung.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Standbuchung konnte nicht angelegt werden", e);
        }
    }

    @Override
    public Optional<Standbuchung> findeById(long id) {
        String sql = "SELECT " + SPALTEN + " FROM standbuchung WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Standbuchung konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Standbuchung> findeByBesuch(long besuchId) {
        String sql = "SELECT " + SPALTEN + " FROM standbuchung WHERE besuch_id=? ORDER BY id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, besuchId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Standbuchung> ergebnis = new ArrayList<>();
                while (rs.next()) {
                    ergebnis.add(lies(rs));
                }
                return ergebnis;
            }
        } catch (SQLException e) {
            throw new PersistenzException("Standbuchungen konnten nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Standbuchung sb) throws SQLException {
        ps.setLong(1, sb.besuchId());
        ps.setLong(2, sb.standId());
        ps.setInt(3, sb.menge());
        ps.setLong(4, sb.einzelpreisCent());
        ps.setLong(5, sb.zeilenpreisCent());
    }

    private static Standbuchung lies(ResultSet rs) throws SQLException {
        return new Standbuchung(
                rs.getLong("id"),
                rs.getLong("besuch_id"),
                rs.getLong("stand_id"),
                rs.getInt("menge"),
                rs.getLong("einzelpreis_cent"),
                rs.getLong("zeilenpreis_cent"));
    }
}

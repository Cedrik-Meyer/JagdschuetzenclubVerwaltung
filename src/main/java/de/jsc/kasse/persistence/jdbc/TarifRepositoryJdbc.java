package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.persistence.PersistenzException;
import de.jsc.kasse.persistence.TarifRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link TarifRepository}. */
public final class TarifRepositoryJdbc implements TarifRepository {

    private static final String SPALTEN =
            "id, stand_id, preis_mitglied_cent, preis_gast_cent, gueltig_ab";

    private final Connection verbindung;

    public TarifRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Tarif anlegen(Tarif tarif) {
        String sql = "INSERT INTO tarif(stand_id, preis_mitglied_cent, preis_gast_cent, gueltig_ab) "
                + "VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, tarif);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return tarif.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Tarif konnte nicht angelegt werden", e);
        }
    }

    @Override
    public Optional<Tarif> findeById(long id) {
        String sql = "SELECT " + SPALTEN + " FROM tarif WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Tarif konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Tarif> findeByStand(long standId) {
        String sql = "SELECT " + SPALTEN + " FROM tarif WHERE stand_id=? ORDER BY gueltig_ab, id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, standId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Tarif> ergebnis = new ArrayList<>();
                while (rs.next()) {
                    ergebnis.add(lies(rs));
                }
                return ergebnis;
            }
        } catch (SQLException e) {
            throw new PersistenzException("Tarife konnten nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Tarif t) throws SQLException {
        ps.setLong(1, t.standId());
        ps.setLong(2, t.preisMitgliedCent());
        ps.setLong(3, t.preisGastCent());
        ps.setString(4, Sql.iso(t.gueltigAb()));
    }

    private static Tarif lies(ResultSet rs) throws SQLException {
        return new Tarif(
                rs.getLong("id"),
                rs.getLong("stand_id"),
                rs.getLong("preis_mitglied_cent"),
                rs.getLong("preis_gast_cent"),
                Sql.datum(rs.getString("gueltig_ab")));
    }
}

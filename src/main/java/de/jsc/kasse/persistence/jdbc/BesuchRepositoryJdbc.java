package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.Besuch;
import de.jsc.kasse.domain.BesuchStatus;
import de.jsc.kasse.persistence.BesuchRepository;
import de.jsc.kasse.persistence.PersistenzException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link BesuchRepository}. */
public final class BesuchRepositoryJdbc implements BesuchRepository {

    private static final String SPALTEN =
            "id, person_id, datum, check_in, check_out, status, gesamt_cent, bezahlt, bezahlt_am";

    private final Connection verbindung;

    public BesuchRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Besuch anlegen(Besuch besuch) {
        String sql = "INSERT INTO besuch(person_id, datum, check_in, check_out, status, "
                + "gesamt_cent, bezahlt, bezahlt_am) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, besuch);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return besuch.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Besuch konnte nicht angelegt werden", e);
        }
    }

    @Override
    public void aktualisieren(Besuch besuch) {
        String sql = "UPDATE besuch SET person_id=?, datum=?, check_in=?, check_out=?, status=?, "
                + "gesamt_cent=?, bezahlt=?, bezahlt_am=? WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            binde(ps, besuch);
            ps.setLong(9, besuch.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenzException("Besuch konnte nicht aktualisiert werden", e);
        }
    }

    @Override
    public Optional<Besuch> findeById(long id) {
        String sql = "SELECT " + SPALTEN + " FROM besuch WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Besuch konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Besuch> findeByPerson(long personId) {
        String sql = "SELECT " + SPALTEN + " FROM besuch WHERE person_id=? ORDER BY id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Besuch> ergebnis = new ArrayList<>();
                while (rs.next()) {
                    ergebnis.add(lies(rs));
                }
                return ergebnis;
            }
        } catch (SQLException e) {
            throw new PersistenzException("Besuche konnten nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Besuch b) throws SQLException {
        ps.setLong(1, b.personId());
        ps.setString(2, Sql.iso(b.datum()));
        ps.setString(3, Sql.iso(b.checkIn()));
        Sql.setzeTextOderNull(ps, 4, Sql.iso(b.checkOut()));
        ps.setString(5, b.status().name());
        ps.setLong(6, b.gesamtCent());
        ps.setInt(7, b.bezahlt() ? 1 : 0);
        Sql.setzeTextOderNull(ps, 8, Sql.iso(b.bezahltAm()));
    }

    private static Besuch lies(ResultSet rs) throws SQLException {
        return new Besuch(
                rs.getLong("id"),
                rs.getLong("person_id"),
                Sql.datum(rs.getString("datum")),
                Sql.zeitpunkt(rs.getString("check_in")),
                Sql.zeitpunkt(rs.getString("check_out")),
                BesuchStatus.valueOf(rs.getString("status")),
                rs.getLong("gesamt_cent"),
                rs.getInt("bezahlt") != 0,
                Sql.zeitpunkt(rs.getString("bezahlt_am")));
    }
}

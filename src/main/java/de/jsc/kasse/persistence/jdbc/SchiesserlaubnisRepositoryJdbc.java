package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.persistence.PersistenzException;
import de.jsc.kasse.persistence.SchiesserlaubnisRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link SchiesserlaubnisRepository}. */
public final class SchiesserlaubnisRepositoryJdbc implements SchiesserlaubnisRepository {

    private static final String SPALTEN =
            "id, person_id, art, nummer, ablaufdatum, ausgestellt_am";

    private final Connection verbindung;

    public SchiesserlaubnisRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Schiesserlaubnis anlegen(Schiesserlaubnis erlaubnis) {
        String sql = "INSERT INTO schiesserlaubnis(person_id, art, nummer, ablaufdatum, "
                + "ausgestellt_am) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, erlaubnis);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return erlaubnis.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Schießerlaubnis konnte nicht angelegt werden", e);
        }
    }

    @Override
    public void aktualisieren(Schiesserlaubnis erlaubnis) {
        String sql = "UPDATE schiesserlaubnis SET person_id=?, art=?, nummer=?, ablaufdatum=?, "
                + "ausgestellt_am=? WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            binde(ps, erlaubnis);
            ps.setLong(6, erlaubnis.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenzException("Schießerlaubnis konnte nicht aktualisiert werden", e);
        }
    }

    @Override
    public Optional<Schiesserlaubnis> findeById(long id) {
        String sql = "SELECT " + SPALTEN + " FROM schiesserlaubnis WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Schießerlaubnis konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Schiesserlaubnis> findeByPerson(long personId) {
        String sql = "SELECT " + SPALTEN + " FROM schiesserlaubnis WHERE person_id=? ORDER BY id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Schiesserlaubnis> ergebnis = new ArrayList<>();
                while (rs.next()) {
                    ergebnis.add(lies(rs));
                }
                return ergebnis;
            }
        } catch (SQLException e) {
            throw new PersistenzException("Schießerlaubnisse konnten nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Schiesserlaubnis e) throws SQLException {
        ps.setLong(1, e.personId());
        ps.setString(2, e.art().name());
        Sql.setzeTextOderNull(ps, 3, e.nummer());
        ps.setString(4, Sql.iso(e.ablaufdatum()));
        Sql.setzeTextOderNull(ps, 5, Sql.iso(e.ausgestelltAm()));
    }

    private static Schiesserlaubnis lies(ResultSet rs) throws SQLException {
        return new Schiesserlaubnis(
                rs.getLong("id"),
                rs.getLong("person_id"),
                LizenzArt.valueOf(rs.getString("art")),
                rs.getString("nummer"),
                Sql.datum(rs.getString("ablaufdatum")),
                Sql.datum(rs.getString("ausgestellt_am")));
    }
}

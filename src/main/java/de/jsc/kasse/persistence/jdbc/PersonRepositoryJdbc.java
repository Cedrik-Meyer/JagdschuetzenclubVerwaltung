package de.jsc.kasse.persistence.jdbc;

import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.persistence.PersistenzException;
import de.jsc.kasse.persistence.PersonRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-Implementierung von {@link PersonRepository}. */
public final class PersonRepositoryJdbc implements PersonRepository {

    private static final String SPALTEN =
            "id, typ, vorname, nachname, geburtsdatum, mitgliedsnummer, beitrittsdatum, "
            + "aktiv, eingeladen_von, kontakt, angelegt_am";

    private final Connection verbindung;

    public PersonRepositoryJdbc(Connection verbindung) {
        this.verbindung = verbindung;
    }

    @Override
    public Person anlegen(Person person) {
        String sql = "INSERT INTO person(typ, vorname, nachname, geburtsdatum, mitgliedsnummer, "
                + "beitrittsdatum, aktiv, eingeladen_von, kontakt, angelegt_am) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = verbindung.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, person);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return person.mitId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenzException("Person konnte nicht angelegt werden", e);
        }
    }

    @Override
    public void aktualisieren(Person person) {
        String sql = "UPDATE person SET typ=?, vorname=?, nachname=?, geburtsdatum=?, "
                + "mitgliedsnummer=?, beitrittsdatum=?, aktiv=?, eingeladen_von=?, kontakt=?, "
                + "angelegt_am=? WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            binde(ps, person);
            ps.setLong(11, person.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenzException("Person konnte nicht aktualisiert werden", e);
        }
    }

    @Override
    public Optional<Person> findeById(long id) {
        String sql = "SELECT " + SPALTEN + " FROM person WHERE id=?";
        try (PreparedStatement ps = verbindung.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lies(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenzException("Person konnte nicht gelesen werden", e);
        }
    }

    @Override
    public List<Person> findeAlle() {
        String sql = "SELECT " + SPALTEN + " FROM person ORDER BY id";
        try (PreparedStatement ps = verbindung.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Person> ergebnis = new ArrayList<>();
            while (rs.next()) {
                ergebnis.add(lies(rs));
            }
            return ergebnis;
        } catch (SQLException e) {
            throw new PersistenzException("Personen konnten nicht gelesen werden", e);
        }
    }

    private static void binde(PreparedStatement ps, Person p) throws SQLException {
        ps.setString(1, p.typ().name());
        ps.setString(2, p.vorname());
        ps.setString(3, p.nachname());
        Sql.setzeTextOderNull(ps, 4, Sql.iso(p.geburtsdatum()));
        Sql.setzeTextOderNull(ps, 5, p.mitgliedsnummer());
        Sql.setzeTextOderNull(ps, 6, Sql.iso(p.beitrittsdatum()));
        ps.setInt(7, p.aktiv() ? 1 : 0);
        Sql.setzeLangOderNull(ps, 8, p.eingeladenVon());
        Sql.setzeTextOderNull(ps, 9, p.kontakt());
        Sql.setzeTextOderNull(ps, 10, Sql.iso(p.angelegtAm()));
    }

    private static Person lies(ResultSet rs) throws SQLException {
        return new Person(
                rs.getLong("id"),
                PersonTyp.valueOf(rs.getString("typ")),
                rs.getString("vorname"),
                rs.getString("nachname"),
                Sql.datum(rs.getString("geburtsdatum")),
                rs.getString("mitgliedsnummer"),
                Sql.datum(rs.getString("beitrittsdatum")),
                rs.getInt("aktiv") != 0,
                Sql.langOderNull(rs, "eingeladen_von"),
                rs.getString("kontakt"),
                Sql.zeitpunkt(rs.getString("angelegt_am")));
    }
}

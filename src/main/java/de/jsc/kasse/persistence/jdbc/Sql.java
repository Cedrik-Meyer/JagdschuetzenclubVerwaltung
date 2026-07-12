package de.jsc.kasse.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kleine Hilfen für die Konvertierung zwischen JDBC und den Domänentypen
 * (ISO-8601-Text ↔ {@link LocalDate}/{@link LocalDateTime}, nullbare Werte).
 */
final class Sql {

    private Sql() {
    }

    static String iso(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    static String iso(LocalDateTime zeitpunkt) {
        return zeitpunkt == null ? null : zeitpunkt.toString();
    }

    static LocalDate datum(String wert) {
        return wert == null ? null : LocalDate.parse(wert);
    }

    static LocalDateTime zeitpunkt(String wert) {
        return wert == null ? null : LocalDateTime.parse(wert);
    }

    /** Liest eine INTEGER-Spalte als nullbaren {@link Long}. */
    static Long langOderNull(ResultSet rs, String spalte) throws SQLException {
        long wert = rs.getLong(spalte);
        return rs.wasNull() ? null : wert;
    }

    static void setzeLangOderNull(PreparedStatement ps, int index, Long wert) throws SQLException {
        if (wert == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, wert);
        }
    }

    static void setzeTextOderNull(PreparedStatement ps, int index, String wert) throws SQLException {
        if (wert == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, wert);
        }
    }
}

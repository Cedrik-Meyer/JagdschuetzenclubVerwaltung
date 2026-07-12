package de.jsc.kasse.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mitglied oder Gast. Wird niemals hart gelöscht — nur über {@code aktiv} deaktiviert
 * (Aufbewahrungspflicht).
 *
 * <p>Felder nach ARCHITEKTUR.md 4.3. {@code id == 0} bedeutet „noch nicht persistiert".
 * Nullbare Felder: {@code geburtsdatum}, {@code mitgliedsnummer}, {@code beitrittsdatum}
 * (nur Mitglied), {@code eingeladenVon} (nur Gast), {@code kontakt}.
 */
public record Person(
        long id,
        PersonTyp typ,
        String vorname,
        String nachname,
        LocalDate geburtsdatum,
        String mitgliedsnummer,
        LocalDate beitrittsdatum,
        boolean aktiv,
        Long eingeladenVon,
        String kontakt,
        LocalDateTime angelegtAm
) {
    /** Kopie mit gesetzter ID (nach dem Persistieren). */
    public Person mitId(long neueId) {
        return new Person(neueId, typ, vorname, nachname, geburtsdatum, mitgliedsnummer,
                beitrittsdatum, aktiv, eingeladenVon, kontakt, angelegtAm);
    }
}

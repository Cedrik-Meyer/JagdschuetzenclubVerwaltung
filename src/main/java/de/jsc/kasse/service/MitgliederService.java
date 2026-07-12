package de.jsc.kasse.service;

import de.jsc.kasse.domain.LizenzArt;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.persistence.PersonRepository;
import de.jsc.kasse.persistence.SchiesserlaubnisRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Anwendungsfälle rund um Personen (Mitglieder und Gäste) und ihre Schießerlaubnisse.
 *
 * <p>Orchestriert die Repositories; kein UI-, kein JavaFX-Bezug. Personen werden niemals
 * hart gelöscht — Deaktivierung setzt nur {@code aktiv = false}.
 */
public final class MitgliederService {

    private final PersonRepository personRepository;
    private final SchiesserlaubnisRepository schiesserlaubnisRepository;

    public MitgliederService(PersonRepository personRepository,
                             SchiesserlaubnisRepository schiesserlaubnisRepository) {
        this.personRepository = personRepository;
        this.schiesserlaubnisRepository = schiesserlaubnisRepository;
    }

    /**
     * Sucht Personen, deren Nachname oder Vorname den Suchbegriff enthält (Teilstring,
     * unabhängig von Groß-/Kleinschreibung) oder deren Mitgliedsnummer ihn enthält.
     */
    public List<Person> sucheMitglied(String suchbegriff) {
        String frage = suchbegriff == null ? "" : suchbegriff.strip().toLowerCase();
        if (frage.isEmpty()) {
            return List.of();
        }
        return personRepository.findeAlle().stream()
                .filter(p -> enthaelt(p.nachname(), frage)
                        || enthaelt(p.vorname(), frage)
                        || enthaelt(p.mitgliedsnummer(), frage))
                .toList();
    }

    public Optional<Person> findeMitgliedNachNummer(String mitgliedsnummer) {
        if (mitgliedsnummer == null) {
            return Optional.empty();
        }
        return personRepository.findeAlle().stream()
                .filter(p -> mitgliedsnummer.equals(p.mitgliedsnummer()))
                .findFirst();
    }

    /**
     * Legt ein Mitglied an. Die Mitgliedsnummer ist Pflicht und muss eindeutig sein.
     *
     * @throws IllegalArgumentException bei fehlender oder bereits vergebener Mitgliedsnummer
     */
    public Person anlegenMitglied(String vorname, String nachname, String mitgliedsnummer,
                                  LocalDate geburtsdatum, LocalDate beitrittsdatum, String kontakt) {
        if (mitgliedsnummer == null || mitgliedsnummer.isBlank()) {
            throw new IllegalArgumentException("Mitgliedsnummer ist erforderlich");
        }
        if (findeMitgliedNachNummer(mitgliedsnummer).isPresent()) {
            throw new IllegalArgumentException("Mitgliedsnummer bereits vergeben: " + mitgliedsnummer);
        }
        Person neu = new Person(0, PersonTyp.MITGLIED, vorname, nachname, geburtsdatum,
                mitgliedsnummer, beitrittsdatum, true, null, kontakt, LocalDateTime.now());
        return personRepository.anlegen(neu);
    }

    /**
     * Legt einen Gast an. {@code eingeladenVon} ist optional; falls gesetzt, muss es auf ein
     * bestehendes Mitglied verweisen.
     *
     * @throws IllegalArgumentException wenn {@code eingeladenVon} kein bestehendes Mitglied ist
     */
    public Person anlegenGast(String vorname, String nachname, LocalDate geburtsdatum,
                              String kontakt, Long eingeladenVon) {
        if (eingeladenVon != null) {
            Person einlader = personRepository.findeById(eingeladenVon)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Einladende Person nicht gefunden: " + eingeladenVon));
            if (einlader.typ() != PersonTyp.MITGLIED) {
                throw new IllegalArgumentException("eingeladen_von muss ein Mitglied sein: " + eingeladenVon);
            }
        }
        Person neu = new Person(0, PersonTyp.GAST, vorname, nachname, geburtsdatum,
                null, null, true, eingeladenVon, kontakt, LocalDateTime.now());
        return personRepository.anlegen(neu);
    }

    /**
     * Ändert die Stammdaten einer bestehenden Person. Eine gesetzte Mitgliedsnummer muss
     * eindeutig bleiben.
     *
     * @throws IllegalArgumentException wenn die Person nicht existiert oder die Mitgliedsnummer
     *                                  bereits einer anderen Person gehört
     */
    public void aktualisierePerson(Person person) {
        personRepository.findeById(person.id())
                .orElseThrow(() -> new IllegalArgumentException("Person nicht gefunden: " + person.id()));
        if (person.mitgliedsnummer() != null) {
            findeMitgliedNachNummer(person.mitgliedsnummer())
                    .filter(andere -> andere.id() != person.id())
                    .ifPresent(andere -> {
                        throw new IllegalArgumentException(
                                "Mitgliedsnummer bereits vergeben: " + person.mitgliedsnummer());
                    });
        }
        personRepository.aktualisieren(person);
    }

    /** Deaktiviert eine Person (setzt {@code aktiv = false}); löscht niemals. */
    public void deaktivierePerson(long id) {
        Person person = personRepository.findeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person nicht gefunden: " + id));
        if (person.aktiv()) {
            personRepository.aktualisieren(mitAktiv(person, false));
        }
    }

    public List<Schiesserlaubnis> lizenzenVon(long personId) {
        return schiesserlaubnisRepository.findeByPerson(personId);
    }

    /**
     * Fügt einer Person eine Schießerlaubnis hinzu.
     *
     * @throws IllegalArgumentException wenn die Person nicht existiert
     */
    public Schiesserlaubnis lizenzHinzufuegen(long personId, LizenzArt art, String nummer,
                                              LocalDate ablaufdatum, LocalDate ausgestelltAm) {
        personRepository.findeById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person nicht gefunden: " + personId));
        return schiesserlaubnisRepository.anlegen(
                new Schiesserlaubnis(0, personId, art, nummer, ablaufdatum, ausgestelltAm));
    }

    /**
     * Trägt für eine bestehende Erlaubnis ein neues Ablaufdatum ein (Weg „neues Datum
     * eintragen" bei abgelaufener/bald ablaufender Lizenz).
     *
     * @throws IllegalArgumentException wenn die Erlaubnis nicht existiert
     */
    public Schiesserlaubnis lizenzAktualisieren(long lizenzId, LocalDate neuesAblaufdatum) {
        Schiesserlaubnis alt = schiesserlaubnisRepository.findeById(lizenzId)
                .orElseThrow(() -> new IllegalArgumentException("Schießerlaubnis nicht gefunden: " + lizenzId));
        Schiesserlaubnis neu = new Schiesserlaubnis(alt.id(), alt.personId(), alt.art(),
                alt.nummer(), neuesAblaufdatum, alt.ausgestelltAm());
        schiesserlaubnisRepository.aktualisieren(neu);
        return neu;
    }

    private static boolean enthaelt(String feld, String frageKlein) {
        return feld != null && feld.toLowerCase().contains(frageKlein);
    }

    private static Person mitAktiv(Person p, boolean aktiv) {
        return new Person(p.id(), p.typ(), p.vorname(), p.nachname(), p.geburtsdatum(),
                p.mitgliedsnummer(), p.beitrittsdatum(), aktiv, p.eingeladenVon(), p.kontakt(),
                p.angelegtAm());
    }
}

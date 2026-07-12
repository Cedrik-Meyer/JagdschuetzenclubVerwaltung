# CLAUDE.md

Projekt-Kontext für den Coding Agent. Diese Datei liegt in der Repo-Wurzel und wird als Dauer-Kontext gelesen. **Verbindliche Detailgrundlage ist `ARCHITEKTUR.md`** — bei Widerspruch gilt `ARCHITEKTUR.md`.

## Projekt

Lokale Java-Desktop-Anwendung zur Verwaltung des Tagesgeschäfts eines Jagdschützenclubs: Mitglieder-/Gästeverwaltung, An-/Abmeldung, Prüfung von Schießerlaubnissen, Standzuteilung, Preisberechnung, dauerhafte Dokumentation. Ein Rechner, offline, ein Bediener.

## Stack

- Java 21 (LTS), JavaFX 21 (`javafx-controls`, `javafx-fxml`)
- SQLite via `org.xerial:sqlite-jdbc`, schlanke Repository-Schicht auf reinem JDBC — **kein ORM**
- Maven, JUnit 5 + AssertJ
- Verpackung am Ende via `jpackage`

## Befehle

```bash
mvn clean test      # Tests — muss grün sein, bevor eine Stage als fertig gilt
mvn javafx:run      # Anwendung starten
mvn clean package   # Build
```

## Architektur-Regeln (nicht verletzen)

Schichten, Abhängigkeiten zeigen immer nach unten:

```
ui  →  service  →  { pricing, lizenz }  →  persistence  →  domain
```

- `domain`, `pricing`, `lizenz` haben **keine** Imports aus `persistence` oder `ui`.
- `pricing` und `lizenz` sind **reine** Module (keine DB, kein JavaFX) und vollständig unit-testbar. Die Tarife/Lizenzen werden von der `service`-Schicht aus der DB aufgelöst und als einfache Werte hineingereicht.
- Paketwurzel: `de.jsc.kasse` (`app`, `domain`, `pricing`, `lizenz`, `persistence`, `service`, `ui`).

## Konventionen

- **Geld** immer als `long` in **Cent**. Niemals `double`/`float` für Beträge.
- **Datum/Zeit**: `LocalDate` / `LocalDateTime` im Code, ISO-8601 in der DB.
- **Domänenbegriffe deutsch** (Mitglied, Gast, Besuch, Stand, Tarif, Schiesserlaubnis, Standbuchung). Technische Bezeichner englisch, wo üblich.
- **Nie hart löschen**: `person` und `besuch` bleiben erhalten (nur `aktiv`-Flag bzw. Status) — Aufbewahrungspflicht.
- **Snapshot-Prinzip**: `standbuchung.einzelpreis_cent`, `zeilenpreis_cent` und `besuch.gesamt_cent` werden bei der Buchung eingefroren. Spätere Tarifänderungen verändern historische Belege nicht.
- Java-21-Features (records, switch expressions, sealed) sind erwünscht.

## Fachliche Festlegungen (bestätigt)

1. **Rabatt**: getrennte `preis_mitglied_cent` / `preis_gast_cent` pro Stand (kein globaler Prozentsatz).
2. **Lizenz → Stand**: vorerst werden **alle Erlaubnisse gleich behandelt** — jede *gültige* Erlaubnis genügt. Das Feld `stand.erforderliche_lizenz` bleibt im Schema, ist aber leer (NULL); der `LizenzPruefer` läuft mit `erforderlich = null`. (Spätere Verschärfung ohne Schema-/Engine-Änderung möglich.)
3. **Abgelaufene Lizenz**: Warnung + neues Datum eintragen **oder** bewusster Override-mit-Vermerk, bevor eine Anmeldung an einem lizenzpflichtigen Stand möglich ist. Keine stille Sperre.
4. **Zahlzeitpunkt**: Positionen bei der Anmeldung erfassen, Preis-Snapshot bei der Bezahlung.
5. **Tarife** sind über das UI durch den Kassenwart pflegbar; Historie via `tarif.gueltig_ab`.

## Stände (Seed)

Skeet (PRO_TAUBE), Trap (PRO_TAUBE), Kipphase (PAUSCHAL), Kugel (PAUSCHAL), Pistole (PAUSCHAL), Laufender Keiler (PAUSCHAL).

## Arbeitsweise

- Streng nach der jeweils aktiven Stage arbeiten. **Scope nicht eigenmächtig erweitern** — außerhalb des Stage-Scope liegende Teile nicht anfassen.
- Für reine Module (`pricing`, `lizenz`) werden Testfälle fachlich abgesegnet, **bevor** implementiert wird.
- Repository-Tests laufen gegen In-Memory-SQLite (`jdbc:sqlite::memory:`).
- Vor Abschluss einer Stage: `mvn clean test` grün, Akzeptanzkriterien der Stage erfüllt.

## Nicht tun

- Kein ORM (Hibernate/JPA), keine Web-Runtime, keine Netzabhängigkeit einführen.
- Schichtgrenzen nicht durchbrechen (kein DB-/UI-Import in `domain`/`pricing`/`lizenz`).
- Beträge nicht als Fließkomma rechnen.
- `person`/`besuch` nicht hart löschen.
- Keine Bibliotheken hinzufügen, ohne dass sie in `ARCHITEKTUR.md` oder dem Stage-Prompt vorgesehen sind.
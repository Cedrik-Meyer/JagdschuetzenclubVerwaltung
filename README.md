# Jagdschützenclub-Verwaltung

Lokale Java-Desktop-Anwendung zur Verwaltung des Tagesgeschäfts eines Jagdschützenclubs
(ein Rechner, offline, ein Bediener). Verbindliche Grundlage ist
[`architektur.md`](architektur.md).

Dieser Stand entspricht **Stage 0**: lauffähiges, leeres Projektgerüst mit korrekter
Paketstruktur und einem startbaren JavaFX-Fenster. Noch keine Datenbank-, Preis- oder
Lizenzlogik.

## Voraussetzungen

- **JDK 21** (LTS)
- **Maven 3.9+** — entweder global installiert (`mvn`) oder der mitgelieferte
  Maven-Wrapper (`./mvnw` bzw. `mvnw.cmd`), der Maven bei Bedarf automatisch lädt.

## Anwendung starten

```bash
mvn javafx:run
```

Öffnet ein Fenster mit dem Titel „Jagdschützenclub-Verwaltung" und einem Platzhalter-Label.

Ohne globales Maven mit dem Wrapper:

```bash
./mvnw javafx:run        # Linux/macOS
mvnw.cmd javafx:run      # Windows
```

## Tests ausführen

```bash
mvn clean test
```

Führt den Smoke-Test aus, der bestätigt, dass die Testinfrastruktur (JUnit 5 + AssertJ)
läuft.

## Build

```bash
mvn clean package
```

## Technologie-Stack

| Bereich    | Wahl                                        |
|------------|---------------------------------------------|
| Sprache    | Java 21 (LTS)                               |
| GUI        | JavaFX 21 (`javafx-controls`, `javafx-fxml`) |
| Datenbank  | SQLite via `org.xerial:sqlite-jdbc`          |
| Tests      | JUnit 5 + AssertJ                            |
| Build      | Maven                                        |

## Paketstruktur

Paketwurzel `de.jsc.kasse` (siehe `architektur.md`, Abschnitt 3):

```
de.jsc.kasse
├── app          // Main / Bootstrap / DI-Verdrahtung
├── domain       // Entities, Enums, Value Objects
├── pricing      // PreisRechner (rein)
├── lizenz       // LizenzPruefer (rein)
├── persistence  // Repository-Interfaces + JDBC-Impls + Schema
├── service      // Use-Case-Services
└── ui           // JavaFX Views + Controller
```

**Abhängigkeitsregel:** `ui → service → { pricing, lizenz } → persistence → domain`.
`domain`, `pricing` und `lizenz` haben keine Imports aus `persistence` oder `ui`.

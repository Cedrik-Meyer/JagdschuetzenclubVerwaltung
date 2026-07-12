-- Schema der Jagdschützenclub-Verwaltung (SQLite).
-- Entspricht ARCHITEKTUR.md Abschnitt 4.3. Geld als INTEGER (Cent), Datum als ISO-8601-Text.
-- Alle Anweisungen sind idempotent (IF NOT EXISTS). Die Ausführung wird durch
-- schema_version gesteuert.

CREATE TABLE IF NOT EXISTS person (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  typ             TEXT NOT NULL CHECK (typ IN ('MITGLIED','GAST')),
  vorname         TEXT NOT NULL,
  nachname        TEXT NOT NULL,
  geburtsdatum    TEXT,
  mitgliedsnummer TEXT UNIQUE,
  beitrittsdatum  TEXT,
  aktiv           INTEGER NOT NULL DEFAULT 1,
  eingeladen_von  INTEGER REFERENCES person(id),
  kontakt         TEXT,
  angelegt_am     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS schiesserlaubnis (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  person_id      INTEGER NOT NULL REFERENCES person(id),
  art            TEXT NOT NULL CHECK (art IN ('JAGDSCHEIN','SPORTSCHUETZENSCHEIN','WBK','SONSTIGE')),
  nummer         TEXT,
  ablaufdatum    TEXT NOT NULL,
  ausgestellt_am TEXT
);

CREATE TABLE IF NOT EXISTS stand (
  id                   INTEGER PRIMARY KEY AUTOINCREMENT,
  name                 TEXT NOT NULL UNIQUE,
  abrechnungsart       TEXT NOT NULL CHECK (abrechnungsart IN ('PRO_TAUBE','PAUSCHAL')),
  erforderliche_lizenz TEXT CHECK (erforderliche_lizenz IN ('JAGDSCHEIN','SPORTSCHUETZENSCHEIN','WBK','SONSTIGE')),
  aktiv                INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tarif (
  id                  INTEGER PRIMARY KEY AUTOINCREMENT,
  stand_id            INTEGER NOT NULL REFERENCES stand(id),
  preis_mitglied_cent INTEGER NOT NULL,
  preis_gast_cent     INTEGER NOT NULL,
  gueltig_ab          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS besuch (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  person_id   INTEGER NOT NULL REFERENCES person(id),
  datum       TEXT NOT NULL,
  check_in    TEXT NOT NULL,
  check_out   TEXT,
  status      TEXT NOT NULL CHECK (status IN ('ANGEMELDET','ABGEMELDET')),
  gesamt_cent INTEGER NOT NULL DEFAULT 0,
  bezahlt     INTEGER NOT NULL DEFAULT 0,
  bezahlt_am  TEXT
);

CREATE TABLE IF NOT EXISTS standbuchung (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  besuch_id        INTEGER NOT NULL REFERENCES besuch(id),
  stand_id         INTEGER NOT NULL REFERENCES stand(id),
  menge            INTEGER NOT NULL,
  einzelpreis_cent INTEGER NOT NULL,
  zeilenpreis_cent INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_version (
  version INTEGER NOT NULL
);

package de.jsc.kasse.ui;

import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/**
 * Modaler Dialog zum Anlegen/Bearbeiten einer Person (Mitglied oder Gast). Sammelt nur
 * Eingaben; die eigentliche Validierung/Persistenz übernimmt der {@code MitgliederService}.
 */
final class PersonBearbeitenDialog {

    /** Eingesammelte Feldwerte. Nicht genutzte Felder je Typ sind {@code null}. */
    record Eingabe(String vorname, String nachname, String mitgliedsnummer,
                   LocalDate geburtsdatum, LocalDate beitrittsdatum, String kontakt,
                   Long eingeladenVon) {
    }

    private PersonBearbeitenDialog() {
    }

    static Optional<Eingabe> zeige(PersonTyp typ, Person vorhanden, List<Person> moeglicheEinlader) {
        boolean istMitglied = typ == PersonTyp.MITGLIED;

        Dialog<Eingabe> dialog = new Dialog<>();
        dialog.setTitle((vorhanden == null ? "Anlegen" : "Bearbeiten")
                + " — " + (istMitglied ? "Mitglied" : "Gast"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField vorname = new TextField();
        TextField nachname = new TextField();
        DatePicker geburtsdatum = new DatePicker();
        TextField kontakt = new TextField();
        TextField mitgliedsnummer = new TextField();
        DatePicker beitrittsdatum = new DatePicker();
        ComboBox<Person> einlader = new ComboBox<>();
        einlader.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person p) {
                return p == null ? "" : p.nachname() + ", " + p.vorname()
                        + (p.mitgliedsnummer() == null ? "" : " (" + p.mitgliedsnummer() + ")");
            }

            @Override
            public Person fromString(String s) {
                return null;
            }
        });

        GridPane gitter = new GridPane();
        gitter.setHgap(8);
        gitter.setVgap(8);
        int zeile = 0;
        gitter.addRow(zeile++, new Label("Vorname:"), vorname);
        gitter.addRow(zeile++, new Label("Nachname:"), nachname);
        gitter.addRow(zeile++, new Label("Geburtsdatum:"), geburtsdatum);
        gitter.addRow(zeile++, new Label("Kontakt:"), kontakt);
        if (istMitglied) {
            gitter.addRow(zeile++, new Label("Mitgliedsnummer:"), mitgliedsnummer);
            gitter.addRow(zeile, new Label("Beitrittsdatum:"), beitrittsdatum);
        } else {
            einlader.getItems().setAll(moeglicheEinlader);
            gitter.addRow(zeile, new Label("Eingeladen von:"), einlader);
        }

        if (vorhanden != null) {
            vorname.setText(vorhanden.vorname());
            nachname.setText(vorhanden.nachname());
            geburtsdatum.setValue(vorhanden.geburtsdatum());
            kontakt.setText(vorhanden.kontakt());
            if (istMitglied) {
                mitgliedsnummer.setText(vorhanden.mitgliedsnummer());
                beitrittsdatum.setValue(vorhanden.beitrittsdatum());
            } else if (vorhanden.eingeladenVon() != null) {
                moeglicheEinlader.stream()
                        .filter(p -> p.id() == vorhanden.eingeladenVon())
                        .findFirst()
                        .ifPresent(einlader::setValue);
            }
        }

        dialog.getDialogPane().setContent(gitter);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        Runnable pruefe = () ->
                okButton.setDisable(vorname.getText().isBlank() || nachname.getText().isBlank());
        vorname.textProperty().addListener((o, a, b) -> pruefe.run());
        nachname.textProperty().addListener((o, a, b) -> pruefe.run());
        pruefe.run();

        dialog.setResultConverter(knopf -> {
            if (knopf != ButtonType.OK) {
                return null;
            }
            Long eingeladenVon = (!istMitglied && einlader.getValue() != null)
                    ? einlader.getValue().id() : null;
            return new Eingabe(
                    vorname.getText().strip(),
                    nachname.getText().strip(),
                    istMitglied ? leerAlsNull(mitgliedsnummer.getText()) : null,
                    geburtsdatum.getValue(),
                    istMitglied ? beitrittsdatum.getValue() : null,
                    leerAlsNull(kontakt.getText()),
                    eingeladenVon);
        });

        return dialog.showAndWait();
    }

    private static String leerAlsNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}

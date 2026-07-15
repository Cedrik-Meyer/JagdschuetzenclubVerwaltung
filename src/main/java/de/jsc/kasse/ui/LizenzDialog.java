package de.jsc.kasse.ui;

import de.jsc.kasse.domain.LizenzArt;
import java.time.LocalDate;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/** Modaler Dialog zum Hinzufügen einer Schießerlaubnis. */
final class LizenzDialog {

    record Eingabe(LizenzArt art, String nummer, LocalDate ablaufdatum, LocalDate ausgestelltAm) {
    }

    private LizenzDialog() {
    }

    static Optional<Eingabe> zeige() {
        Dialog<Eingabe> dialog = new Dialog<>();
        dialog.setTitle("Schießerlaubnis hinzufügen");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<LizenzArt> art = new ComboBox<>();
        art.getItems().setAll(LizenzArt.values());
        art.setValue(LizenzArt.JAGDSCHEIN);
        TextField nummer = new TextField();
        DatePicker ablaufdatum = new DatePicker();
        DatePicker ausgestelltAm = new DatePicker();

        GridPane gitter = new GridPane();
        gitter.setHgap(8);
        gitter.setVgap(8);
        gitter.addRow(0, new Label("Art:"), art);
        gitter.addRow(1, new Label("Nummer:"), nummer);
        gitter.addRow(2, new Label("Ablaufdatum:"), ablaufdatum);
        gitter.addRow(3, new Label("Ausgestellt am:"), ausgestelltAm);
        dialog.getDialogPane().setContent(gitter);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        Runnable pruefe = () -> okButton.setDisable(art.getValue() == null || ablaufdatum.getValue() == null);
        art.valueProperty().addListener((o, a, b) -> pruefe.run());
        ablaufdatum.valueProperty().addListener((o, a, b) -> pruefe.run());
        pruefe.run();

        dialog.setResultConverter(knopf -> {
            if (knopf != ButtonType.OK) {
                return null;
            }
            String n = nummer.getText();
            return new Eingabe(art.getValue(), (n == null || n.isBlank()) ? null : n.strip(),
                    ablaufdatum.getValue(), ausgestelltAm.getValue());
        });

        return dialog.showAndWait();
    }
}

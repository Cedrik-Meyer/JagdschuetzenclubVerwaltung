package de.jsc.kasse.ui;

import java.time.LocalDate;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Kleiner Dialog zur Eingabe eines einzelnen Datums (z. B. neues Ablaufdatum). */
final class DatumDialog {

    private DatumDialog() {
    }

    static Optional<LocalDate> zeige(String titel, String beschriftung, LocalDate initial) {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle(titel);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker datum = new DatePicker(initial);
        HBox inhalt = new HBox(8, new Label(beschriftung), datum);
        dialog.getDialogPane().setContent(inhalt);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(datum.getValue() == null);
        datum.valueProperty().addListener((o, a, b) -> okButton.setDisable(b == null));

        dialog.setResultConverter(knopf -> knopf == ButtonType.OK ? datum.getValue() : null);
        return dialog.showAndWait();
    }
}

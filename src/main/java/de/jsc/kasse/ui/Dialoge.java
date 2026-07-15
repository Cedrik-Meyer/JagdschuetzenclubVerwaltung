package de.jsc.kasse.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/** Kleine Helfer für benutzerfreundliche Alerts (keine Stacktraces im UI). */
final class Dialoge {

    private Dialoge() {
    }

    static void fehler(String nachricht) {
        Alert alert = new Alert(AlertType.ERROR, nachricht, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Fehler");
        alert.showAndWait();
    }

    static boolean bestaetige(String nachricht) {
        Alert alert = new Alert(AlertType.CONFIRMATION, nachricht, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Bestätigung");
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }
}

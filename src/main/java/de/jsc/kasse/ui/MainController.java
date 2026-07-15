package de.jsc.kasse.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;

/**
 * Controller des Hauptfensters (Reiter-Navigation). Hält keine Geschäftslogik; die
 * Composition Root im {@code app}-Paket setzt die Inhalte der Reiter.
 */
public class MainController {

    @FXML
    private Tab mitgliederTab;

    @FXML
    private Tab stammdatenTab;

    /** Setzt den Inhalt des Mitglieder-Reiters (von der Composition Root injiziert). */
    public void setMitgliederInhalt(Node inhalt) {
        mitgliederTab.setContent(inhalt);
    }

    /** Setzt den Inhalt des Stammdaten-Reiters (von der Composition Root injiziert). */
    public void setStammdatenInhalt(Node inhalt) {
        stammdatenTab.setContent(inhalt);
    }
}

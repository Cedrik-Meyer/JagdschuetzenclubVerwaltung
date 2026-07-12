package de.jsc.kasse.app;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der Anwendung (Stage 0).
 *
 * <p>Öffnet ein leeres Fenster mit Titel und Platzhalter-Label. Noch keine
 * Geschäftslogik, keine Datenbank, keine echten Views.
 */
public class App extends Application {

    static final String TITEL = "Jagdschützenclub-Verwaltung";

    @Override
    public void start(Stage stage) {
        Label platzhalter = new Label("Jagdschützenclub-Verwaltung — Projektgerüst (Stage 0)");

        StackPane wurzel = new StackPane(platzhalter);
        StackPane.setAlignment(platzhalter, Pos.CENTER);

        stage.setTitle(TITEL);
        stage.setScene(new Scene(wurzel, 640, 400));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

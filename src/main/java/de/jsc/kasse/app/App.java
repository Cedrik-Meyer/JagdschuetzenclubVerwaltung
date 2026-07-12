package de.jsc.kasse.app;

import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>Öffnet beim Start die SQLite-Datenbank (Schema wird angelegt/migriert, Stände geseedet)
 * und zeigt ein Fenster mit Titel und Platzhalter-Label. Noch keine echten Views.
 */
public class App extends Application {

    static final String TITEL = "Jagdschützenclub-Verwaltung";
    private static final Path STANDARD_DB = Path.of("kasse.db");

    private Datenbank datenbank;

    @Override
    public void start(Stage stage) {
        datenbank = Datenbank.ausDatei(STANDARD_DB);
        int anzahlStaende = new StandRepositoryJdbc(datenbank.verbindung()).findeAlle().size();

        Label platzhalter = new Label(
                "Jagdschützenclub-Verwaltung — Datenbank bereit (" + anzahlStaende + " Stände)");

        StackPane wurzel = new StackPane(platzhalter);
        StackPane.setAlignment(platzhalter, Pos.CENTER);

        stage.setTitle(TITEL);
        stage.setScene(new Scene(wurzel, 640, 400));
        stage.show();
    }

    @Override
    public void stop() {
        if (datenbank != null) {
            datenbank.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

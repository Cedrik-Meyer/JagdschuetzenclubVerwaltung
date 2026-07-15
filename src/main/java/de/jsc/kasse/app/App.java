package de.jsc.kasse.app;

import de.jsc.kasse.lizenz.LizenzPruefer;
import de.jsc.kasse.persistence.Datenbank;
import de.jsc.kasse.persistence.PersonRepository;
import de.jsc.kasse.persistence.SchiesserlaubnisRepository;
import de.jsc.kasse.persistence.StandRepository;
import de.jsc.kasse.persistence.TarifRepository;
import de.jsc.kasse.persistence.jdbc.PersonRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.SchiesserlaubnisRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.StandRepositoryJdbc;
import de.jsc.kasse.persistence.jdbc.TarifRepositoryJdbc;
import de.jsc.kasse.service.MitgliederService;
import de.jsc.kasse.service.StammdatenService;
import de.jsc.kasse.ui.MainController;
import de.jsc.kasse.ui.MitgliederController;
import de.jsc.kasse.ui.StammdatenController;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Composition Root der Anwendung: öffnet die SQLite-Datenbank (Schema/Migration/Seed), baut
 * Repositories und Services und verdrahtet sie mit den JavaFX-Controllern. Kein DI-Framework.
 *
 * <p>Der Datenbankpfad ist über die System-Property {@code jsc.db} konfigurierbar; Default ist
 * {@code ~/.jagdschuetzenclub/kasse.db}.
 */
public class App extends Application {

    static final String TITEL = "Jagdschützenclub-Verwaltung";

    private Datenbank datenbank;

    @Override
    public void start(Stage stage) throws IOException {
        datenbank = oeffneDatenbank();
        Connection verbindung = datenbank.verbindung();

        // Repositories + Services (Composition Root)
        PersonRepository personRepository = new PersonRepositoryJdbc(verbindung);
        SchiesserlaubnisRepository schiesserlaubnisRepository =
                new SchiesserlaubnisRepositoryJdbc(verbindung);
        StandRepository standRepository = new StandRepositoryJdbc(verbindung);
        TarifRepository tarifRepository = new TarifRepositoryJdbc(verbindung);
        MitgliederService mitgliederService =
                new MitgliederService(personRepository, schiesserlaubnisRepository);
        StammdatenService stammdatenService =
                new StammdatenService(standRepository, tarifRepository);
        LizenzPruefer lizenzPruefer = new LizenzPruefer();

        // Mitglieder-View mit injiziertem Controller
        FXMLLoader mitgliederLoader =
                new FXMLLoader(getClass().getResource("/de/jsc/kasse/ui/Mitglieder.fxml"));
        mitgliederLoader.setController(new MitgliederController(mitgliederService, lizenzPruefer));
        Parent mitgliederInhalt = mitgliederLoader.load();

        // Stammdaten-View mit injiziertem Controller
        FXMLLoader stammdatenLoader =
                new FXMLLoader(getClass().getResource("/de/jsc/kasse/ui/Stammdaten.fxml"));
        stammdatenLoader.setController(new StammdatenController(stammdatenService));
        Parent stammdatenInhalt = stammdatenLoader.load();

        // Hauptfenster
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/de/jsc/kasse/ui/Main.fxml"));
        Parent wurzel = mainLoader.load();
        MainController mainController = mainLoader.getController();
        mainController.setMitgliederInhalt(mitgliederInhalt);
        mainController.setStammdatenInhalt(stammdatenInhalt);

        stage.setTitle(TITEL);
        stage.setScene(new Scene(wurzel, 1000, 640));
        stage.show();
    }

    private Datenbank oeffneDatenbank() throws IOException {
        String konfiguriert = System.getProperty("jsc.db");
        Path pfad = (konfiguriert != null && !konfiguriert.isBlank())
                ? Path.of(konfiguriert)
                : Path.of(System.getProperty("user.home"), ".jagdschuetzenclub", "kasse.db");
        Path verzeichnis = pfad.toAbsolutePath().getParent();
        if (verzeichnis != null) {
            Files.createDirectories(verzeichnis);
        }
        return Datenbank.ausDatei(pfad);
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

package de.jsc.kasse.ui;

import de.jsc.kasse.domain.Stand;
import de.jsc.kasse.domain.Tarif;
import de.jsc.kasse.service.StammdatenService;
import de.jsc.kasse.ui.format.Geld;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Stammdaten-Reiter: Stand- und Tarifpflege (Kassenwart). Dünner Controller — die Fachlogik
 * liegt im {@link StammdatenService}; Euro↔Cent nur hier über {@link Geld}.
 */
public class StammdatenController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final StammdatenService service;

    /** Der zum heutigen Tag gültige Tarif je Stand (falls vorhanden) — für die Preisspalten. */
    private final Map<Long, Tarif> aktuelleTarife = new HashMap<>();

    @FXML private TableView<Stand> staendeTabelle;
    @FXML private Button aktivSchaltenBtn;
    @FXML private Label detailTitel;
    @FXML private TableView<Tarif> historieTabelle;
    @FXML private Label aktuellerTarifHinweis;
    @FXML private TextField mitgliedspreisFeld;
    @FXML private TextField gastpreisFeld;
    @FXML private DatePicker gueltigAbFeld;
    @FXML private Button tarifAnlegenBtn;

    public StammdatenController(StammdatenService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        baueStandSpalten();
        baueHistorieSpalten();
        staendeTabelle.setPlaceholder(new Label("Keine Stände"));
        historieTabelle.setPlaceholder(new Label("Keine Tarife"));

        aktivSchaltenBtn.disableProperty().bind(keineAuswahl(staendeTabelle));
        tarifAnlegenBtn.disableProperty().bind(keineAuswahl(staendeTabelle));

        staendeTabelle.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> aktualisiereDetail());
        aktivSchaltenBtn.setOnAction(e -> aktivUmschalten());
        tarifAnlegenBtn.setOnAction(e -> tarifAnlegen());

        aktualisiereStaende();
        aktualisiereDetail();
    }

    // --- Tabellenaufbau ---

    private void baueStandSpalten() {
        TableColumn<Stand, String> name = spalte("Name", Stand::name);
        TableColumn<Stand, String> art = spalte("Abrechnung", s -> s.abrechnungsart().name());
        TableColumn<Stand, String> status = spalte("Status", s -> s.aktiv() ? "aktiv" : "inaktiv");
        TableColumn<Stand, String> mitglied = spalte("Preis Mitglied", s -> preisText(s, true));
        TableColumn<Stand, String> gast = spalte("Preis Gast", s -> preisText(s, false));
        staendeTabelle.getColumns().setAll(List.of(name, art, status, mitglied, gast));
    }

    private void baueHistorieSpalten() {
        TableColumn<Tarif, String> gueltigAb = spalte("Gültig ab", t -> t.gueltigAb().format(DATUM));
        TableColumn<Tarif, String> mitglied =
                spalte("Mitgliedspreis", t -> Geld.formatiere(t.preisMitgliedCent()));
        TableColumn<Tarif, String> gast =
                spalte("Gastpreis", t -> Geld.formatiere(t.preisGastCent()));
        historieTabelle.getColumns().setAll(List.of(gueltigAb, mitglied, gast));
    }

    private static <T> TableColumn<T, String> spalte(String titel, Function<T, String> wert) {
        TableColumn<T, String> spalte = new TableColumn<>(titel);
        spalte.setCellValueFactory(c -> new SimpleStringProperty(wert.apply(c.getValue())));
        return spalte;
    }

    private String preisText(Stand stand, boolean mitglied) {
        Tarif tarif = aktuelleTarife.get(stand.id());
        if (tarif == null) {
            return "—";
        }
        return Geld.formatiere(mitglied ? tarif.preisMitgliedCent() : tarif.preisGastCent());
    }

    // --- Aktualisierung ---

    private void aktualisiereStaende() {
        List<Stand> staende = service.alleStaende(false);
        LocalDate heute = LocalDate.now();
        aktuelleTarife.clear();
        for (Stand stand : staende) {
            try {
                aktuelleTarife.put(stand.id(), service.aktuellerTarif(stand.id(), heute));
            } catch (IllegalStateException ohneTarif) {
                // Für heute ist kein Tarif hinterlegt -> Spalte zeigt "—".
            }
        }
        staendeTabelle.getItems().setAll(staende);
    }

    private void aktualisiereDetail() {
        Stand stand = ausgewaehlterStand();
        if (stand == null) {
            detailTitel.setText("Kein Stand ausgewählt");
            aktuellerTarifHinweis.setText("");
            historieTabelle.getItems().clear();
            return;
        }
        detailTitel.setText("Tarife: " + stand.name());
        aktivSchaltenBtn.setText(stand.aktiv() ? "Deaktivieren" : "Aktivieren");
        historieTabelle.getItems().setAll(service.tarifHistorie(stand.id()));
        aktuellerTarifHinweis.setText(aktuelleTarife.containsKey(stand.id())
                ? "" : "Für heute ist kein Tarif hinterlegt.");
    }

    // --- Aktionen ---

    private void aktivUmschalten() {
        Stand stand = ausgewaehlterStand();
        if (stand == null) {
            return;
        }
        boolean neu = !stand.aktiv();
        boolean ok = Dialoge.bestaetige("Stand \"" + stand.name() + "\" wirklich auf "
                + (neu ? "aktiv" : "inaktiv") + " setzen?");
        if (!ok) {
            return;
        }
        try {
            service.standAktivSetzen(stand.id(), neu);
        } catch (RuntimeException ex) {
            Dialoge.fehler(ex.getMessage());
            return;
        }
        aktualisiereStaende();
        waehleStandNachId(stand.id());
    }

    private void tarifAnlegen() {
        Stand stand = ausgewaehlterStand();
        if (stand == null) {
            return;
        }
        LocalDate gueltigAb = gueltigAbFeld.getValue();
        if (gueltigAb == null) {
            Dialoge.fehler("Bitte ein „gültig ab\"-Datum wählen.");
            return;
        }

        long mitgliedspreisCent;
        long gastpreisCent;
        try {
            mitgliedspreisCent = Geld.parse(mitgliedspreisFeld.getText());
            gastpreisCent = Geld.parse(gastpreisFeld.getText());
        } catch (IllegalArgumentException ex) {
            Dialoge.fehler("Ungültiger Preis. Bitte einen Euro-Betrag wie z. B. 5,00 eingeben.");
            return;
        }
        if (mitgliedspreisCent < 0 || gastpreisCent < 0) {
            Dialoge.fehler("Preise dürfen nicht negativ sein.");
            return;
        }

        boolean datumBelegt = service.tarifHistorie(stand.id()).stream()
                .anyMatch(t -> t.gueltigAb().equals(gueltigAb));
        if (datumBelegt) {
            Dialoge.fehler("Für diesen Stand existiert bereits ein Tarif mit gültig ab "
                    + gueltigAb.format(DATUM) + ".");
            return;
        }

        try {
            service.neuenTarifAnlegen(stand.id(), mitgliedspreisCent, gastpreisCent, gueltigAb);
        } catch (RuntimeException ex) {
            Dialoge.fehler(ex.getMessage());
            return;
        }

        mitgliedspreisFeld.clear();
        gastpreisFeld.clear();
        gueltigAbFeld.setValue(null);
        aktualisiereStaende();
        waehleStandNachId(stand.id());
    }

    // --- Hilfen ---

    private Stand ausgewaehlterStand() {
        return staendeTabelle.getSelectionModel().getSelectedItem();
    }

    private void waehleStandNachId(long standId) {
        staendeTabelle.getItems().stream()
                .filter(s -> s.id() == standId)
                .findFirst()
                .ifPresent(s -> staendeTabelle.getSelectionModel().select(s));
    }

    private static BooleanBinding keineAuswahl(TableView<?> tabelle) {
        return tabelle.getSelectionModel().selectedItemProperty().isNull();
    }
}

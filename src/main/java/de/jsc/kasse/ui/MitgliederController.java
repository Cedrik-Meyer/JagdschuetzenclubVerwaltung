package de.jsc.kasse.ui;

import de.jsc.kasse.domain.LizenzBewertung;
import de.jsc.kasse.domain.Person;
import de.jsc.kasse.domain.PersonTyp;
import de.jsc.kasse.domain.Schiesserlaubnis;
import de.jsc.kasse.lizenz.LizenzPruefer;
import de.jsc.kasse.lizenz.LizenzStatus;
import de.jsc.kasse.service.MitgliederService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Mitglieder-/Gästeverwaltung inkl. Lizenzpflege. Dünner Controller: Anzeige und Interaktion,
 * die Fachlogik liegt im {@link MitgliederService} und im {@link LizenzPruefer}.
 */
public class MitgliederController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FILTER_ALLE = "Alle";
    private static final String FILTER_MITGLIEDER = "Mitglieder";
    private static final String FILTER_GAESTE = "Gäste";

    private final MitgliederService service;
    private final LizenzPruefer lizenzPruefer;

    @FXML private TextField suchfeld;
    @FXML private ComboBox<String> typFilter;
    @FXML private CheckBox nurAktive;
    @FXML private Button neuMitgliedBtn;
    @FXML private Button neuGastBtn;
    @FXML private Button bearbeitenBtn;
    @FXML private Button deaktivierenBtn;
    @FXML private TableView<Person> personenTabelle;
    @FXML private TableView<Schiesserlaubnis> lizenzTabelle;
    @FXML private Button lizenzHinzufuegenBtn;
    @FXML private Button ablaufAktualisierenBtn;

    public MitgliederController(MitgliederService service, LizenzPruefer lizenzPruefer) {
        this.service = service;
        this.lizenzPruefer = lizenzPruefer;
    }

    @FXML
    private void initialize() {
        bauePersonenSpalten();
        baueLizenzSpalten();
        personenTabelle.setPlaceholder(new Label("Keine Personen"));
        lizenzTabelle.setPlaceholder(new Label("Keine Person ausgewählt"));

        typFilter.getItems().setAll(FILTER_ALLE, FILTER_MITGLIEDER, FILTER_GAESTE);
        typFilter.setValue(FILTER_ALLE);

        bearbeitenBtn.disableProperty().bind(keineAuswahl(personenTabelle));
        deaktivierenBtn.disableProperty().bind(keineAuswahl(personenTabelle));
        lizenzHinzufuegenBtn.disableProperty().bind(keineAuswahl(personenTabelle));
        ablaufAktualisierenBtn.disableProperty().bind(keineAuswahl(lizenzTabelle));

        suchfeld.textProperty().addListener((o, a, b) -> aktualisierePersonen());
        typFilter.valueProperty().addListener((o, a, b) -> aktualisierePersonen());
        nurAktive.selectedProperty().addListener((o, a, b) -> aktualisierePersonen());
        personenTabelle.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> aktualisiereLizenzen());

        neuMitgliedBtn.setOnAction(e -> neuePerson(PersonTyp.MITGLIED));
        neuGastBtn.setOnAction(e -> neuePerson(PersonTyp.GAST));
        bearbeitenBtn.setOnAction(e -> bearbeitePerson());
        deaktivierenBtn.setOnAction(e -> deaktivierePerson());
        lizenzHinzufuegenBtn.setOnAction(e -> lizenzHinzufuegen());
        ablaufAktualisierenBtn.setOnAction(e -> ablaufAktualisieren());

        aktualisierePersonen();
    }

    // --- Tabellenaufbau ---

    private void bauePersonenSpalten() {
        TableColumn<Person, String> typ = spalte("Typ", p -> p.typ().name());
        TableColumn<Person, String> nachname = spalte("Nachname", Person::nachname);
        TableColumn<Person, String> vorname = spalte("Vorname", Person::vorname);
        TableColumn<Person, String> nummer = spalte("Mitgliedsnr.", Person::mitgliedsnummer);
        TableColumn<Person, String> aktiv = spalte("Status", p -> p.aktiv() ? "aktiv" : "inaktiv");
        personenTabelle.getColumns().setAll(List.of(typ, nachname, vorname, nummer, aktiv));
    }

    private void baueLizenzSpalten() {
        TableColumn<Schiesserlaubnis, String> art = spalte("Art", l -> l.art().name());
        TableColumn<Schiesserlaubnis, String> nummer = spalte("Nummer", Schiesserlaubnis::nummer);
        TableColumn<Schiesserlaubnis, String> ablauf =
                spalte("Ablauf", l -> l.ablaufdatum().format(DATUM));
        TableColumn<Schiesserlaubnis, String> status = spalte("Status", l -> statusText(bewerte(l)));
        lizenzTabelle.getColumns().setAll(List.of(art, nummer, ablauf, status));

        lizenzTabelle.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Schiesserlaubnis lizenz, boolean leer) {
                super.updateItem(lizenz, leer);
                setStyle(leer || lizenz == null ? "" : farbeFuer(bewerte(lizenz)));
            }
        });
    }

    private static <T> TableColumn<T, String> spalte(String titel,
                                                     java.util.function.Function<T, String> wert) {
        TableColumn<T, String> spalte = new TableColumn<>(titel);
        spalte.setCellValueFactory(c -> new SimpleStringProperty(wert.apply(c.getValue())));
        return spalte;
    }

    // --- Aktualisierung ---

    private void aktualisierePersonen() {
        String suche = suchfeld.getText();
        List<Person> basis = (suche == null || suche.isBlank())
                ? service.findeAlle() : service.sucheMitglied(suche);
        String filter = typFilter.getValue();
        boolean nurAktiv = nurAktive.isSelected();

        List<Person> gefiltert = basis.stream()
                .filter(p -> passtZumTyp(p, filter))
                .filter(p -> !nurAktiv || p.aktiv())
                .toList();
        personenTabelle.getItems().setAll(gefiltert);
    }

    private static boolean passtZumTyp(Person p, String filter) {
        return switch (filter == null ? FILTER_ALLE : filter) {
            case FILTER_MITGLIEDER -> p.typ() == PersonTyp.MITGLIED;
            case FILTER_GAESTE -> p.typ() == PersonTyp.GAST;
            default -> true;
        };
    }

    private void aktualisiereLizenzen() {
        Person person = ausgewaehltePerson();
        if (person == null) {
            lizenzTabelle.getItems().clear();
        } else {
            lizenzTabelle.getItems().setAll(service.lizenzenVon(person.id()));
        }
    }

    // --- Aktionen ---

    private void neuePerson(PersonTyp typ) {
        List<Person> einlader = typ == PersonTyp.GAST ? nurMitglieder() : List.of();
        PersonBearbeitenDialog.zeige(typ, null, einlader).ifPresent(e -> fuehreAus(() -> {
            if (typ == PersonTyp.MITGLIED) {
                service.anlegenMitglied(e.vorname(), e.nachname(), e.mitgliedsnummer(),
                        e.geburtsdatum(), e.beitrittsdatum(), e.kontakt());
            } else {
                service.anlegenGast(e.vorname(), e.nachname(), e.geburtsdatum(), e.kontakt(),
                        e.eingeladenVon());
            }
            aktualisierePersonen();
        }));
    }

    private void bearbeitePerson() {
        Person sel = ausgewaehltePerson();
        if (sel == null) {
            return;
        }
        List<Person> einlader = nurMitglieder().stream().filter(p -> p.id() != sel.id()).toList();
        PersonBearbeitenDialog.zeige(sel.typ(), sel, einlader).ifPresent(e -> fuehreAus(() -> {
            boolean istMitglied = sel.typ() == PersonTyp.MITGLIED;
            Person aktualisiert = new Person(sel.id(), sel.typ(), e.vorname(), e.nachname(),
                    e.geburtsdatum(),
                    istMitglied ? e.mitgliedsnummer() : null,
                    istMitglied ? e.beitrittsdatum() : null,
                    sel.aktiv(),
                    istMitglied ? null : e.eingeladenVon(),
                    e.kontakt(), sel.angelegtAm());
            service.aktualisierePerson(aktualisiert);
            aktualisierePersonen();
        }));
    }

    private void deaktivierePerson() {
        Person sel = ausgewaehltePerson();
        if (sel == null) {
            return;
        }
        boolean ok = Dialoge.bestaetige("Person \"" + sel.vorname() + " " + sel.nachname()
                + "\" wirklich deaktivieren? Der Datensatz bleibt erhalten (kein Löschen).");
        if (ok) {
            fuehreAus(() -> {
                service.deaktivierePerson(sel.id());
                aktualisierePersonen();
            });
        }
    }

    private void lizenzHinzufuegen() {
        Person sel = ausgewaehltePerson();
        if (sel == null) {
            return;
        }
        LizenzDialog.zeige().ifPresent(e -> fuehreAus(() -> {
            service.lizenzHinzufuegen(sel.id(), e.art(), e.nummer(), e.ablaufdatum(), e.ausgestelltAm());
            aktualisiereLizenzen();
        }));
    }

    private void ablaufAktualisieren() {
        Schiesserlaubnis lizenz = lizenzTabelle.getSelectionModel().getSelectedItem();
        if (lizenz == null) {
            return;
        }
        DatumDialog.zeige("Neues Ablaufdatum", "Neues Ablaufdatum:", lizenz.ablaufdatum())
                .ifPresent(neu -> fuehreAus(() -> {
                    service.lizenzAktualisieren(lizenz.id(), neu);
                    aktualisiereLizenzen();
                }));
    }

    // --- Hilfen ---

    /** Führt eine Service-Aktion aus und zeigt Fehler als freundlichen Alert (kein Stacktrace). */
    private void fuehreAus(Runnable aktion) {
        try {
            aktion.run();
        } catch (RuntimeException ex) {
            Dialoge.fehler(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private Person ausgewaehltePerson() {
        return personenTabelle.getSelectionModel().getSelectedItem();
    }

    private List<Person> nurMitglieder() {
        return service.findeAlle().stream().filter(p -> p.typ() == PersonTyp.MITGLIED).toList();
    }

    private LizenzStatus bewerte(Schiesserlaubnis lizenz) {
        return lizenzPruefer.pruefe(List.of(lizenz), null, LocalDate.now());
    }

    private static String statusText(LizenzStatus status) {
        return switch (status.bewertung()) {
            case ABGELAUFEN -> "abgelaufen";
            case FEHLT -> "—";
            case GUELTIG -> status.warnung()
                    ? "läuft " + status.ablaufdatum().getYear() + " ab" : "gültig";
        };
    }

    private static String farbeFuer(LizenzStatus status) {
        if (status.bewertung() == LizenzBewertung.ABGELAUFEN) {
            return "-fx-background-color: #f8d0d0;"; // rot
        }
        if (status.warnung()) {
            return "-fx-background-color: #fff3c4;"; // gelb
        }
        return "";
    }

    private static javafx.beans.binding.BooleanBinding keineAuswahl(TableView<?> tabelle) {
        return tabelle.getSelectionModel().selectedItemProperty().isNull();
    }
}

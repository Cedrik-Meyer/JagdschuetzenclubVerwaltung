package de.jsc.kasse.ui.format;

/**
 * Reiner Formatierungs-/Parse-Helfer für Geldbeträge. Beträge werden intern als {@code long}
 * in Cent gehalten; die Euro-Darstellung existiert nur in der View.
 *
 * <p>Ohne JavaFX-, DB- oder sonstige Abhängigkeiten — vollständig unit-testbar.
 */
public final class Geld {

    private Geld() {
    }

    /** Formatiert Cent als Euro-Betrag, z. B. {@code 1234 -> "12,34 €"}, {@code -5 -> "-0,05 €"}. */
    public static String formatiere(long cent) {
        long betrag = Math.abs(cent);
        long euro = betrag / 100;
        long rest = betrag % 100;
        String vorzeichen = cent < 0 ? "-" : "";
        String restText = rest < 10 ? "0" + rest : Long.toString(rest);
        return vorzeichen + euro + "," + restText + " €";
    }

    /**
     * Parst eine Euro-Eingabe in Cent. Erlaubt Komma oder Punkt als Dezimaltrenner, ein
     * optionales {@code €} und umgebende Leerzeichen (z. B. {@code "12,34 €" -> 1234},
     * {@code "12" -> 1200}, {@code "12,5" -> 1250}).
     *
     * @throws IllegalArgumentException bei leerer oder ungültiger Eingabe
     */
    public static long parse(String eingabe) {
        if (eingabe == null) {
            throw new IllegalArgumentException("Betrag fehlt");
        }
        String text = eingabe.strip().replace("€", "").strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Betrag fehlt");
        }

        boolean negativ = text.startsWith("-");
        if (negativ) {
            text = text.substring(1).strip();
        }
        text = text.replace('.', ',');

        String euroTeil;
        String centTeil;
        int komma = text.indexOf(',');
        if (komma < 0) {
            euroTeil = text;
            centTeil = "00";
        } else {
            euroTeil = text.substring(0, komma);
            centTeil = text.substring(komma + 1);
        }
        if (euroTeil.isEmpty()) {
            euroTeil = "0";
        }
        if (centTeil.length() == 1) {
            centTeil = centTeil + "0";
        }
        if (centTeil.length() != 2 || !nurZiffern(euroTeil) || !nurZiffern(centTeil)) {
            throw new IllegalArgumentException("Kein gültiger Euro-Betrag: " + eingabe);
        }

        long cent = Long.parseLong(euroTeil) * 100 + Long.parseLong(centTeil);
        return negativ ? -cent : cent;
    }

    private static boolean nurZiffern(String s) {
        return !s.isEmpty() && s.chars().allMatch(Character::isDigit);
    }
}

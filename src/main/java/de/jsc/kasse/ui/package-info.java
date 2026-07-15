/**
 * JavaFX-Präsentationsschicht: FXML-Views und Controller (dünn gehalten).
 *
 * <p>Nutzt {@code service}, {@code domain} und die reinen Module ({@code lizenz}); importiert
 * <strong>nicht</strong> {@code persistence}. Die Verdrahtung erfolgt in der Composition Root
 * ({@code app}). Geldbeträge werden als {@code long} Cent gehalten und nur hier via
 * {@link de.jsc.kasse.ui.format.Geld} zu Euro formatiert.
 */
package de.jsc.kasse.ui;

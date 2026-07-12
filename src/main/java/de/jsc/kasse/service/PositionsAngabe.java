package de.jsc.kasse.service;

/**
 * Eingabe einer Position beim Check-out: welcher Stand und (bei PRO_TAUBE) wie viele Tauben.
 *
 * <p>{@code menge} ist nur für PRO_TAUBE relevant; bei PAUSCHAL wird sie ignoriert und intern
 * auf 1 gesetzt (Tagespreis je Stand).
 */
public record PositionsAngabe(long standId, int menge) {
}

package eu.simpl.vocabulary.domain.validation;

/**
 * A blocking validation error (D10).
 *
 * @param detail the error text (D9)
 * @param location where it is (D65)
 */
public record ValidationError(String detail, String location) {}

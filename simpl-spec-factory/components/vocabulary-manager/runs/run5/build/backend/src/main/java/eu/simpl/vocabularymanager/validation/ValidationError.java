package eu.simpl.vocabularymanager.validation;

/**
 * A blocking validation error (D10): a syntax error or one of the semantic rules S1 to S7.
 *
 * @param detail fixed detail text of the rule
 * @param location where in the content the error is
 */
public record ValidationError(String detail, String location) {}

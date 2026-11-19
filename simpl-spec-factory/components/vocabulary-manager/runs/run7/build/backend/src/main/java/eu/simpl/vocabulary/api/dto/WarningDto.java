package eu.simpl.vocabulary.api.dto;

/**
 * Wire form of a validation warning (D10).
 *
 * @param code warning code
 * @param description text
 * @param location location (D65)
 */
public record WarningDto(String code, String description, String location) {}

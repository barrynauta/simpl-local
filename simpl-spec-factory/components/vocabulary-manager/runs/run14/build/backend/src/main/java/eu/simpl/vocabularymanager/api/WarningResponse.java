package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Wire form of a validation warning (D10).
 *
 * @param code warning code
 * @param description description
 * @param location location
 */
@JsonPropertyOrder({"code", "description", "location"})
public record WarningResponse(String code, String description, String location) {}

package eu.simpl.vocabularymanager.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** A non-blocking validation warning, returned once with the 201 response and never stored (decision D10). */
@JsonPropertyOrder({"code", "description", "location"})
public record ValidationWarning(WarningCode code, String description, String location) {}

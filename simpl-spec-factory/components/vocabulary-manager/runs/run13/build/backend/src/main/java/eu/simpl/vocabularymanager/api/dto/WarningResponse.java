package eu.simpl.vocabularymanager.api.dto;

/** Wire form of a validation warning (OpenAPI VocabularyValidationWarning, D10). */
public record WarningResponse(String code, String description, String location) {}

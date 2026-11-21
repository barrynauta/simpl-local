package eu.simpl.vocabularymanager.api.dto;

/** Wire form of a vocabulary search result (OpenAPI VocabularySearchItem, D38). */
public record SearchItemResponse(
        String href, String contentHref, String name, Integer version, String status, Boolean latest) {}

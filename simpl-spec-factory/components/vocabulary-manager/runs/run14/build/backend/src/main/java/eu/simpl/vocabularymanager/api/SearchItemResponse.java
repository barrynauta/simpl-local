package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Wire form of a search result (OpenAPI schema VocabularySearchItem, D38, D184).
 *
 * @param href link to the version
 * @param contentHref link to the content
 * @param name vocabulary name
 * @param version version number
 * @param status vocabulary status for latest results, else version status
 * @param latest true for a reference without version
 */
@JsonPropertyOrder({"href", "contentHref", "name", "version", "status", "latest"})
public record SearchItemResponse(
        String href, String contentHref, String name, long version, String status, boolean latest) {}

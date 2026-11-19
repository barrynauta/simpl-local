package eu.simpl.vocabulary.api.dto;

/**
 * Wire form of a search result: schema VocabularySearchItem (D26, D38).
 *
 * @param href link
 * @param contentHref content URL
 * @param name name
 * @param version version number
 * @param status status
 * @param latest true for a reference without version
 */
public record SearchItemDto(
        String href, String contentHref, String name, long version, String status, boolean latest) {}

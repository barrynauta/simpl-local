package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * Filters and sort of the vocabulary list (D19, D20, D23).
 *
 * @param nameContains case-insensitive contains filter, or null
 * @param status status filter, or null
 * @param creationFrom inclusive lower bound, or null
 * @param creationTo exclusive upper bound, or null
 * @param updatedFrom inclusive lower bound, or null
 * @param updatedTo exclusive upper bound, or null
 * @param sort sort field
 * @param descending sort direction
 */
public record VocabularyFilter(
        String nameContains,
        VocabularyStatus status,
        Instant creationFrom,
        Instant creationTo,
        Instant updatedFrom,
        Instant updatedTo,
        SortField sort,
        boolean descending) {

    /** Sort fields of the vocabulary and version lists. */
    public enum SortField {
        NAME,
        STATUS,
        CREATION_DATE,
        LAST_UPDATED,
        VERSION
    }
}

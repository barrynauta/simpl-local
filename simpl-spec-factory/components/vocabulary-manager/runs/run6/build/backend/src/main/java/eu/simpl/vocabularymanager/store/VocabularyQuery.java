package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;

/**
 * Filters, order and page of the vocabulary list (decisions D19, D20, D22, D23).
 *
 * @param nameContains case-insensitive part of the name, or null
 * @param status vocabulary status, or null
 * @param creationDate creation date range, or null
 * @param lastUpdated last update date range, or null
 * @param sort the order
 * @param offset items to skip
 * @param limit items to return
 */
public record VocabularyQuery(
        String nameContains,
        VocabularyStatus status,
        InstantRange creationDate,
        InstantRange lastUpdated,
        SortOrder sort,
        long offset,
        int limit) {}

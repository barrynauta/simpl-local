package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;

/**
 * Filters, order and page of GET /vocabularies (D19, D20, D22, D23). Each date filter is a half-open range
 * [from, to); null means no filter.
 */
public record ListQuery(
        String nameContains,
        VocabularyStatus status,
        Instant createdFrom,
        Instant createdTo,
        Instant updatedFrom,
        Instant updatedTo,
        String sortField,
        boolean descending,
        long offset,
        int limit) {}

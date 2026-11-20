package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;

/**
 * Filters, sort and paging of a list query (D19, D20, D22, D23). Date filters are half-open UTC
 * ranges: a date is its whole day, a date-time is its second.
 */
public record ListQuery(
        String nameContains,
        VocabularyStatus status,
        TimeRange creationDate,
        TimeRange lastUpdated,
        String sortField,
        boolean ascending,
        long page,
        int pageSize) {

    /** A half-open time range [from, to). */
    public record TimeRange(Instant from, Instant to) {}

    public long offset() {
        return (page - 1) * pageSize;
    }
}

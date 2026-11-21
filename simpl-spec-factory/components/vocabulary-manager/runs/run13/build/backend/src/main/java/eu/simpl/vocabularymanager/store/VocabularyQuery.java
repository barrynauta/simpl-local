package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;

/**
 * Filters, sort and page of the vocabulary list (D19, D20, D22, D23, D58). Null filter values are absent.
 *
 * @param nameContains lower-case text the name must contain, case-insensitively
 * @param creation creation date range, from inclusive, to exclusive
 * @param lastUpdate last update date range, from inclusive, to exclusive
 */
public record VocabularyQuery(
        String nameContains,
        VocabularyStatus status,
        TimeRange creation,
        TimeRange lastUpdate,
        SortField sort,
        boolean descending,
        int page,
        int pageSize) {

    /** Sortable fields of GET /vocabularies. */
    public enum SortField {
        NAME,
        STATUS,
        CREATION_DATE,
        LAST_UPDATED
    }

    /** A UTC time range: a whole day (date filter) or one second (date-time filter). */
    public record TimeRange(Instant from, Instant toExclusive) {}

    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}

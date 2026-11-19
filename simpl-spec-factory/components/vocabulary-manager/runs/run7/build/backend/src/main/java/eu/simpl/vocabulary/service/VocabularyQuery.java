package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.domain.model.VocabularyStatus;

/**
 * Filters, sort and page of GET /vocabularies (D19, D20, D22, D23).
 *
 * @param nameContains case-insensitive 'contains' filter, may be null
 * @param status status filter, may be null
 * @param creationDate creation date filter, may be null
 * @param lastUpdated last update date filter, may be null
 * @param sort sort value, for example "-name"
 * @param page page number
 * @param pageSize page size
 */
public record VocabularyQuery(
        String nameContains,
        VocabularyStatus status,
        DateFilter creationDate,
        DateFilter lastUpdated,
        String sort,
        long page,
        int pageSize) {}

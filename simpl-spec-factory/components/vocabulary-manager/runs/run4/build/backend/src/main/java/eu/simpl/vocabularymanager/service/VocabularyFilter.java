package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Filters of GET /vocabularies; null means no filter.
 *
 * @param nameContains case-insensitive 'contains' on the name (decision D19)
 * @param status vocabulary status
 * @param creationDate creation date filter (decision D20)
 * @param lastUpdated last update date filter (decision D20)
 */
public record VocabularyFilter(
        String nameContains, VocabularyStatus status, DateFilter creationDate, DateFilter lastUpdated)
        implements Predicate<Vocabulary> {

    public static final VocabularyFilter NONE = new VocabularyFilter(null, null, null, null);

    @Override
    public boolean test(Vocabulary vocabulary) {
        if (nameContains != null
                && !vocabulary.name().toLowerCase(Locale.ROOT).contains(nameContains.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (status != null && vocabulary.status() != status) {
            return false;
        }
        if (creationDate != null && !creationDate.matches(vocabulary.creationDate())) {
            return false;
        }
        return lastUpdated == null || lastUpdated.matches(vocabulary.lastUpdateDate());
    }
}

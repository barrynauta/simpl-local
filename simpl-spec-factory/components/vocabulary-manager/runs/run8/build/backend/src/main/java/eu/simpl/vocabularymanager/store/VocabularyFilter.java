package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.dateTime;
import static eu.simpl.vocabularymanager.store.StoreModel.str;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;
import java.util.Locale;

/**
 * Filters of GET /vocabularies, turned into SPARQL FILTER clauses (D19, D20, D85).
 *
 * @param nameContains case-insensitive 'contains' on the name, or null
 * @param status vocabulary status, or null
 * @param creationDate creation time range, or null
 * @param lastUpdated last update time range, or null
 */
public record VocabularyFilter(
        String nameContains, VocabularyStatus status, TimeRange creationDate, TimeRange lastUpdated) {

    /**
     * A half-open time range [from, to).
     *
     * @param from inclusive start
     * @param to exclusive end
     */
    public record TimeRange(Instant from, Instant to) {}

    /**
     * @return the FILTER clauses, empty when there are no filters
     */
    public String toSparql() {
        StringBuilder sparql = new StringBuilder();
        if (nameContains != null) {
            sparql.append("FILTER(CONTAINS(LCASE(?name), ")
                    .append(str(nameContains.toLowerCase(Locale.ROOT)))
                    .append("))\n");
        }
        if (status != null) {
            sparql.append("FILTER(?status = ").append(str(status.name())).append(")\n");
        }
        appendRange(sparql, "?creationDate", creationDate);
        appendRange(sparql, "?lastUpdateDate", lastUpdated);
        return sparql.toString();
    }

    private static void appendRange(StringBuilder sparql, String variable, TimeRange range) {
        if (range == null) {
            return;
        }
        sparql.append("FILTER(")
                .append(variable)
                .append(" >= ")
                .append(dateTime(range.from()))
                .append(" && ")
                .append(variable)
                .append(" < ")
                .append(dateTime(range.to()))
                .append(")\n");
    }
}

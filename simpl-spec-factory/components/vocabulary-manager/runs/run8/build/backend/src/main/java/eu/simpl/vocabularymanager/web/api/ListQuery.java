package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.store.VocabularyFilter;
import eu.simpl.vocabularymanager.store.VocabularyFilter.TimeRange;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.routing.QueryParameters;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Parses the filters and sort values of the list operations (D19, D20, D21, D23). */
public final class ListQuery {

    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    private static final Map<String, String> VOCABULARY_SORT = Map.of(
            "name", "ASC(?name)",
            "-name", "DESC(?name)",
            "status", "ASC(?status) ASC(?name)",
            "-status", "DESC(?status) ASC(?name)",
            "creationDate", "ASC(?creationDate) ASC(?name)",
            "-creationDate", "DESC(?creationDate) ASC(?name)",
            "lastUpdated", "ASC(?lastUpdateDate) ASC(?name)",
            "-lastUpdated", "DESC(?lastUpdateDate) ASC(?name)");
    private static final Map<String, String> VERSION_SORT = Map.of(
            "name", "DESC(?num)",
            "-name", "DESC(?num)",
            "status", "ASC(?verStatus) DESC(?num)",
            "-status", "DESC(?verStatus) DESC(?num)",
            "creationDate", "ASC(?verCreationDate) DESC(?num)",
            "-creationDate", "DESC(?verCreationDate) DESC(?num)");
    private static final String VOCABULARY_SORT_VALUES =
            "[name, -name, status, -status, creationDate, -creationDate, lastUpdated, -lastUpdated]";
    private static final String VERSION_SORT_VALUES = "[name, -name, status, -status, creationDate, -creationDate]";

    private ListQuery() {}

    /**
     * @param parameters the query parameters
     * @param issues collects invalid values
     * @return the filters of GET /vocabularies
     */
    public static VocabularyFilter vocabularyFilter(QueryParameters parameters, List<ProblemIssue> issues) {
        String name = parameters.first("name");
        if (name != null && !NAME_FILTER.matcher(name).matches()) {
            issues.add(ProblemIssue.schemaViolation(
                    "query", "name", "Query parameter 'name' must have 1 to 64 letters or digits."));
            name = null;
        }
        VocabularyStatus status = null;
        String statusValue = parameters.first("status");
        if (statusValue != null) {
            status = VocabularyStatus.parse(statusValue).orElse(null);
            if (status == null) {
                issues.add(ProblemIssue.schemaViolation(
                        "query", "status", "Query parameter 'status' must be one of [ACTIVE, DEPRECATED]."));
            }
        }
        TimeRange creation = timeRange(parameters.first("creationDate"), "creationDate", issues);
        TimeRange lastUpdated = timeRange(parameters.first("lastUpdated"), "lastUpdated", issues);
        return new VocabularyFilter(name, status, creation, lastUpdated);
    }

    /**
     * @param parameters the query parameters
     * @param issues collects invalid values
     * @return the ORDER BY clause of GET /vocabularies, name ascending by default
     */
    public static String vocabularySort(QueryParameters parameters, List<ProblemIssue> issues) {
        return sort(parameters, issues, VOCABULARY_SORT, "name", VOCABULARY_SORT_VALUES);
    }

    /**
     * @param parameters the query parameters
     * @param issues collects invalid values
     * @return the ORDER BY clause of GET .../versions, version descending by default
     */
    public static String versionSort(QueryParameters parameters, List<ProblemIssue> issues) {
        String value = parameters.first("sort");
        if (value == null) {
            return "DESC(?num)";
        }
        return sort(parameters, issues, VERSION_SORT, "name", VERSION_SORT_VALUES);
    }

    private static String sort(
            QueryParameters parameters,
            List<ProblemIssue> issues,
            Map<String, String> clauses,
            String defaultValue,
            String allowed) {
        String value = parameters.first("sort");
        String clause = clauses.get(value == null ? defaultValue : value);
        if (clause == null) {
            issues.add(ProblemIssue.schemaViolation(
                    "query", "sort", "Query parameter 'sort' must be one of " + allowed + "."));
            return clauses.get(defaultValue);
        }
        return clause;
    }

    /**
     * D20: a date matches the whole UTC day; a date-time matches to the second.
     *
     * @param value the filter value, or null
     * @param parameter the parameter name
     * @param issues collects invalid values
     * @return the range, or null
     */
    static TimeRange timeRange(String value, String parameter, List<ProblemIssue> issues) {
        if (value == null) {
            return null;
        }
        String message = "Query parameter '" + parameter + "' must be a valid date (YYYY-MM-DD) or date-time.";
        if (!DATE_FILTER.matcher(value).matches()) {
            issues.add(ProblemIssue.schemaViolation("query", parameter, message));
            return null;
        }
        try {
            if (value.length() == 10) {
                Instant start = LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
                return new TimeRange(start, start.plus(1, ChronoUnit.DAYS));
            }
            Instant moment = parseDateTime(value).truncatedTo(ChronoUnit.SECONDS);
            return new TimeRange(moment, moment.plusSeconds(1));
        } catch (DateTimeParseException e) {
            issues.add(ProblemIssue.schemaViolation("query", parameter, message));
            return null;
        }
    }

    private static Instant parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        }
    }
}

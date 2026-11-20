package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyFilter.SortField;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Parsers of the query parameters of the list operations (D19 to D23, D91). */
public final class QueryParsers {

    /** Maximum page size (D22). */
    public static final int PAGE_SIZE_MAX = 100;

    /** Default page size (D22). */
    public static final int PAGE_SIZE_DEFAULT = 20;

    /** Name filter pattern (D19). */
    public static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]+$");

    /** Maximum length of the name filter (D19). */
    public static final int NAME_FILTER_MAX = 64;

    /** Date filter pattern of the OpenAPI. */
    public static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");

    /** Sort values of GET /vocabularies. */
    public static final List<String> VOCABULARY_SORTS = List.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");

    /** Sort values of GET /vocabularies/{vocabularyName}/versions. */
    public static final List<String> VERSION_SORTS =
            List.of("name", "-name", "status", "-status", "creationDate", "-creationDate");

    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private QueryParsers() {}

    /**
     * A sort selection.
     *
     * @param field sort field
     * @param descending direction
     */
    public record Sort(SortField field, boolean descending) {}

    /** Filters and sort of GET /vocabularies. */
    static VocabularyFilter vocabularyFilter(Map<String, String> parameters, List<Issue> issues) {
        String name = parameters.get("name");
        if (name != null
                && (name.isEmpty()
                        || name.length() > NAME_FILTER_MAX
                        || !NAME_FILTER.matcher(name).matches())) {
            issues.add(Issue.schemaViolation(
                    "query", "name", "Query parameter 'name' must be 1 to 64 letters or digits.", name));
            name = null;
        }
        VocabularyStatus status = null;
        if (parameters.containsKey("status")) {
            status = VocabularyStatus.fromWire(parameters.get("status")).orElse(null);
            if (status == null) {
                issues.add(Issue.schemaViolation(
                        "query",
                        "status",
                        "Query parameter 'status' must be ACTIVE or DEPRECATED.",
                        parameters.get("status")));
            }
        }
        Instant[] created = dateRange(parameters, "creationDate", issues);
        Instant[] updated = dateRange(parameters, "lastUpdated", issues);
        Sort sort = sort(parameters.get("sort"), VOCABULARY_SORTS, issues);
        return new VocabularyFilter(
                name, status, created[0], created[1], updated[0], updated[1], sort.field(), sort.descending());
    }

    /** Sort of GET .../versions; default version number descending (D23). */
    static Sort versionSort(String value, List<Issue> issues) {
        if (value == null) {
            return new Sort(SortField.VERSION, true);
        }
        return sort(value, VERSION_SORTS, issues);
    }

    private static Sort sort(String value, List<String> allowed, List<Issue> issues) {
        if (value == null) {
            return new Sort(SortField.NAME, false);
        }
        if (!allowed.contains(value)) {
            issues.add(Issue.schemaViolation(
                    "query", "sort", "Query parameter 'sort' must be one of " + allowed + ".", value));
            return new Sort(SortField.NAME, false);
        }
        boolean descending = value.startsWith("-");
        String field = descending ? value.substring(1) : value;
        SortField sortField = switch (field) {
            case "status" -> SortField.STATUS;
            case "creationDate" -> SortField.CREATION_DATE;
            case "lastUpdated" -> SortField.LAST_UPDATED;
            default -> SortField.NAME;
        };
        return new Sort(sortField, descending);
    }

    /**
     * A date filter as a half-open range: a date covers its UTC day, a date-time its second (D20, D58).
     *
     * @return from and to, both null when absent or invalid
     */
    static Instant[] dateRange(Map<String, String> parameters, String name, List<Issue> issues) {
        String value = parameters.get(name);
        if (value == null) {
            return new Instant[] {null, null};
        }
        try {
            if (!DATE_FILTER.matcher(value).matches()) {
                throw new DateTimeParseException("pattern", value, 0);
            }
            if (value.length() == 10) {
                Instant from = LocalDate.parse(value, STRICT_DATE)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
                return new Instant[] {from, from.plus(1, ChronoUnit.DAYS)};
            }
            Instant from = parseDateTime(value).truncatedTo(ChronoUnit.SECONDS);
            return new Instant[] {from, from.plusSeconds(1)};
        } catch (DateTimeParseException ex) {
            issues.add(Issue.schemaViolation(
                    "query", name, "Query parameter '" + name + "' must be a valid date or date-time.", value));
            return new Instant[] {null, null};
        }
    }

    private static Instant parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC);
        }
    }

    /** page and pageSize (D22, D91). */
    static PageRequest page(Map<String, String> parameters, List<Issue> issues) {
        int page = 1;
        int pageSize = PAGE_SIZE_DEFAULT;
        if (parameters.containsKey("page")) {
            Integer value = RequestChecks.positiveInt(parameters.get("page"));
            if (value == null) {
                issues.add(Issue.schemaViolation(
                        "query",
                        "page",
                        "Query parameter 'page' must be an integer from 1 to 2147483647.",
                        parameters.get("page")));
            } else {
                page = value;
            }
        }
        if (parameters.containsKey("pageSize")) {
            Integer value = RequestChecks.positiveInt(parameters.get("pageSize"));
            if (value == null || value > PAGE_SIZE_MAX) {
                issues.add(Issue.schemaViolation(
                        "query",
                        "pageSize",
                        "Query parameter 'pageSize' must be an integer from 1 to 100.",
                        parameters.get("pageSize")));
            } else {
                pageSize = value;
            }
        }
        return new PageRequest(page, pageSize);
    }
}

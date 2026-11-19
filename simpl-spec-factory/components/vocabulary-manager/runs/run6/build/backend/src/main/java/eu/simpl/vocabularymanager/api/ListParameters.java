package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.store.InstantRange;
import eu.simpl.vocabularymanager.store.SortOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.MultiValueMap;

/** Parsing of the list filters and sort values (decisions D19, D20, D23, D58). */
public final class ListParameters {

    /** Query parameters of GET /vocabularies. */
    public static final Set<String> VOCABULARY_LIST =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");

    /** Query parameters of GET /vocabularies/{name}/versions. */
    public static final Set<String> VERSION_LIST = Set.of("sort", "page", "pageSize");

    /** Query parameters of GET /external-vocabularies. */
    public static final Set<String> EXTERNAL_LIST = Set.of("page", "pageSize");

    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private static final Map<String, SortOrder> VOCABULARY_SORTS = sorts(true);
    private static final Map<String, SortOrder> VERSION_SORTS = sorts(false);

    private ListParameters() {}

    private static Map<String, SortOrder> sorts(boolean withLastUpdated) {
        Map<String, SortOrder> map = new LinkedHashMap<>();
        map.put("name", new SortOrder(SortOrder.Field.NAME, false));
        map.put("-name", new SortOrder(SortOrder.Field.NAME, true));
        map.put("status", new SortOrder(SortOrder.Field.STATUS, false));
        map.put("-status", new SortOrder(SortOrder.Field.STATUS, true));
        map.put("creationDate", new SortOrder(SortOrder.Field.CREATION_DATE, false));
        map.put("-creationDate", new SortOrder(SortOrder.Field.CREATION_DATE, true));
        if (withLastUpdated) {
            map.put("lastUpdated", new SortOrder(SortOrder.Field.LAST_UPDATED, false));
            map.put("-lastUpdated", new SortOrder(SortOrder.Field.LAST_UPDATED, true));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    /**
     * Reads the name filter.
     *
     * @param parameters the query parameters
     * @param issues the issue list
     * @return the filter or null
     */
    public static String nameFilter(MultiValueMap<String, String> parameters, List<Issue> issues) {
        String value = parameters.getFirst("name");
        if (value != null && !NAME_FILTER.matcher(value).matches()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "name", "Query parameter 'name' must be 1 to 64 letters or digits."));
            return null;
        }
        return value;
    }

    /**
     * Reads the status filter.
     *
     * @param parameters the query parameters
     * @param issues the issue list
     * @return the status or null
     */
    public static VocabularyStatus statusFilter(MultiValueMap<String, String> parameters, List<Issue> issues) {
        String value = parameters.getFirst("status");
        if (value == null) {
            return null;
        }
        return VocabularyStatus.fromWire(value).orElseGet(() -> {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "status", "Query parameter 'status' must be one of [ACTIVE, DEPRECATED]."));
            return null;
        });
    }

    /**
     * Reads a date filter (decision D20): a date matches its whole UTC day; a date-time matches to the second.
     *
     * @param parameters the query parameters
     * @param name creationDate or lastUpdated
     * @param issues the issue list
     * @return the range or null
     */
    public static InstantRange dateFilter(MultiValueMap<String, String> parameters, String name, List<Issue> issues) {
        String value = parameters.getFirst(name);
        if (value == null) {
            return null;
        }
        InstantRange range = DATE_FILTER.matcher(value).matches() ? parseDate(value) : null;
        if (range == null) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, name, "Query parameter '" + name + "' must be an ISO 8601 date or date-time."));
        }
        return range;
    }

    static InstantRange parseDate(String value) {
        try {
            if (value.length() == 10) {
                Instant start = LocalDate.parse(value, STRICT_DATE)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
                return new InstantRange(start, start.plus(1, ChronoUnit.DAYS));
            }
            Instant instant;
            try {
                instant = OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException e) {
                instant = LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            }
            Instant second = instant.truncatedTo(ChronoUnit.SECONDS);
            return new InstantRange(second, second.plusSeconds(1));
        } catch (DateTimeParseException | ArithmeticException e) {
            return null;
        }
    }

    /**
     * Reads the sort of the vocabulary list; default name ascending (decision D23).
     *
     * @param parameters the query parameters
     * @param issues the issue list
     * @return the order
     */
    public static SortOrder vocabularySort(MultiValueMap<String, String> parameters, List<Issue> issues) {
        return sort(parameters, VOCABULARY_SORTS, new SortOrder(SortOrder.Field.NAME, false), issues);
    }

    /**
     * Reads the sort of the version list; default version descending (decision D23).
     *
     * @param parameters the query parameters
     * @param issues the issue list
     * @return the order
     */
    public static SortOrder versionSort(MultiValueMap<String, String> parameters, List<Issue> issues) {
        return sort(parameters, VERSION_SORTS, new SortOrder(SortOrder.Field.VERSION, true), issues);
    }

    private static SortOrder sort(
            MultiValueMap<String, String> parameters,
            Map<String, SortOrder> allowed,
            SortOrder fallback,
            List<Issue> issues) {
        String value = parameters.getFirst("sort");
        if (value == null) {
            return fallback;
        }
        SortOrder order = allowed.get(value);
        if (order == null) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "sort", "Query parameter 'sort' must be one of " + allowed.keySet() + "."));
            return fallback;
        }
        return order;
    }

    /**
     * Gives the filter and sort parameters to repeat in paging links, in a fixed order.
     *
     * @param parameters the query parameters
     * @param names the names to keep
     * @return the parameters
     */
    public static Map<String, String> linkParameters(MultiValueMap<String, String> parameters, List<String> names) {
        Map<String, String> kept = new LinkedHashMap<>();
        for (String name : names) {
            String value = parameters.getFirst(name);
            if (value != null) {
                kept.put(name, value);
            }
        }
        return kept;
    }
}

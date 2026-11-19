package eu.simpl.vocabulary.api.request;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.service.DateFilter;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Rules for query parameter values (D19 to D23). Each method adds an issue on an invalid value. */
public final class QueryRules {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final Set<String> VOCABULARY_SORTS = Set.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    public static final Set<String> VERSION_SORTS =
            Set.of("name", "-name", "status", "-status", "creationDate", "-creationDate");

    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,100}$");

    private QueryRules() {}

    /**
     * Page number: integer, at least 1, default 1 (D22).
     *
     * @param query the query
     * @param issues issue sink
     * @return the page
     */
    public static long page(QueryParameters query, List<Issue> issues) {
        Optional<String> value = query.get("page");
        if (value.isEmpty()) {
            return 1;
        }
        Optional<BigInteger> number = integer(value.get());
        if (number.isEmpty() || number.get().signum() <= 0) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "page", "Query parameter 'page' must be an integer of at least 1."));
            return 1;
        }
        return number.get().bitLength() > 62 ? Long.MAX_VALUE / 2 : number.get().longValue();
    }

    /**
     * Page size: integer from 1 to 100, default 20 (D22).
     *
     * @param query the query
     * @param issues issue sink
     * @return the page size
     */
    public static int pageSize(QueryParameters query, List<Issue> issues) {
        Optional<String> value = query.get("pageSize");
        if (value.isEmpty()) {
            return DEFAULT_PAGE_SIZE;
        }
        Optional<BigInteger> number = integer(value.get());
        if (number.isEmpty()
                || number.get().signum() <= 0
                || number.get().compareTo(BigInteger.valueOf(MAX_PAGE_SIZE)) > 0) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "pageSize", "Query parameter 'pageSize' must be an integer from 1 to 100."));
            return DEFAULT_PAGE_SIZE;
        }
        return number.get().intValue();
    }

    private static Optional<BigInteger> integer(String value) {
        if (!DIGITS.matcher(value).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigInteger(value));
    }

    /**
     * Sort value from a closed set (D23).
     *
     * @param query the query
     * @param allowed allowed values
     * @param issues issue sink
     * @return the sort value or null
     */
    public static String sort(QueryParameters query, Set<String> allowed, List<Issue> issues) {
        Optional<String> value = query.get("sort");
        if (value.isEmpty()) {
            return null;
        }
        if (!allowed.contains(value.get())) {
            issues.add(
                    Issue.schemaViolation(Issue.IN_QUERY, "sort", "Query parameter 'sort' has an unsupported value."));
            return null;
        }
        return value.get();
    }

    /**
     * Name filter: letters and digits, 1 to 64 characters (D19).
     *
     * @param query the query
     * @param issues issue sink
     * @return the filter or null
     */
    public static String nameFilter(QueryParameters query, List<Issue> issues) {
        Optional<String> value = query.get("name");
        if (value.isEmpty()) {
            return null;
        }
        if (!NAME_FILTER.matcher(value.get()).matches()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "name", "Query parameter 'name' must be 1 to 64 letters or digits."));
            return null;
        }
        return value.get();
    }

    /**
     * Status filter (D3).
     *
     * @param query the query
     * @param issues issue sink
     * @return the status or null
     */
    public static VocabularyStatus status(QueryParameters query, List<Issue> issues) {
        Optional<String> value = query.get("status");
        if (value.isEmpty()) {
            return null;
        }
        Optional<VocabularyStatus> status = VocabularyStatus.parse(value.get());
        if (status.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, "status", "Query parameter 'status' must be ACTIVE or DEPRECATED."));
            return null;
        }
        return status.get();
    }

    /**
     * Date filter (D20).
     *
     * @param query the query
     * @param name parameter name
     * @param issues issue sink
     * @return the filter or null
     */
    public static DateFilter date(QueryParameters query, String name, List<Issue> issues) {
        Optional<String> value = query.get(name);
        if (value.isEmpty()) {
            return null;
        }
        Optional<DateFilter> filter = DateFilter.parse(value.get());
        if (filter.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_QUERY, name, "Query parameter '" + name + "' must be a valid date or date-time."));
            return null;
        }
        return filter.get();
    }
}

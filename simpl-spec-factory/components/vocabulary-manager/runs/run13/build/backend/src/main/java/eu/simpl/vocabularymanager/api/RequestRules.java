package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.store.VocabularyQuery.TimeRange;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The decided request rules (D19, D20, D22, D37, D49, D73, D91, D112, D130). Each check adds an issue
 * and returns null when the value breaks the rule. The constants are compared with openapi-decided.yaml
 * by a test (D93).
 */
public final class RequestRules {

    public static final String NAME_PATTERN = "^[A-Z][a-zA-Z0-9]*$";
    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 64;
    public static final String NAME_FILTER_PATTERN = "^[A-Za-z0-9]+$";
    public static final int NAME_FILTER_MAX = 64;
    public static final int TEXT_MAX = 1000;
    public static final int PAGE_SIZE_MAX = 100;
    public static final int PAGE_SIZE_DEFAULT = 20;
    public static final long INT32_MAX = Integer.MAX_VALUE;
    public static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}(T.*)?$";
    public static final int MAX_ALIASES = 20;

    private static final Pattern NAME = Pattern.compile(NAME_PATTERN);
    private static final Pattern NAME_FILTER = Pattern.compile(NAME_FILTER_PATTERN);
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern DATE = Pattern.compile(DATE_PATTERN);
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private RequestRules() {}

    /** A vocabulary or external vocabulary name (D37). */
    public static String name(String value, String in, String field, List<ProblemIssue> issues) {
        if (value == null || value.isEmpty()) {
            issues.add(ProblemIssue.schemaViolation(in, field, "'" + field + "' is required.", null));
            return null;
        }
        if (value.length() < NAME_MIN
                || value.length() > NAME_MAX
                || !NAME.matcher(value).matches()) {
            issues.add(ProblemIssue.schemaViolation(
                    in,
                    field,
                    "'" + field + "' must be 3 to 64 characters, start with an upper-case letter and contain only"
                            + " letters and digits.",
                    shorten(value)));
            return null;
        }
        return value;
    }

    /** A positive 32-bit integer given as digits only (D49, D91, D112). */
    public static Integer positiveInt(String value, String in, String field, long max, List<ProblemIssue> issues) {
        if (value == null || !DIGITS.matcher(value).matches()) {
            issues.add(ProblemIssue.schemaViolation(
                    in, field, "'" + field + "' must be a whole number from 1 to " + max + ".", shorten(value)));
            return null;
        }
        BigInteger number = new BigInteger(value);
        if (number.signum() <= 0 || number.compareTo(BigInteger.valueOf(max)) > 0) {
            issues.add(ProblemIssue.schemaViolation(
                    in, field, "'" + field + "' must be a whole number from 1 to " + max + ".", shorten(value)));
            return null;
        }
        return number.intValue();
    }

    /** Required text of 1 to 1000 characters after trimming (D37). Returns the trimmed text. */
    public static String text(String value, String field, boolean required, List<ProblemIssue> issues) {
        if (value == null) {
            if (required) {
                issues.add(ProblemIssue.schemaViolation("body", field, "'" + field + "' is required.", null));
            }
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            if (required) {
                issues.add(ProblemIssue.schemaViolation("body", field, "'" + field + "' must not be empty.", null));
            }
            return null;
        }
        if (trimmed.codePointCount(0, trimmed.length()) > TEXT_MAX) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", field, "'" + field + "' must be at most 1000 characters.", null));
            return null;
        }
        return trimmed;
    }

    /** The name filter of D19. */
    public static String nameFilter(String value, List<ProblemIssue> issues) {
        if (value.isEmpty()
                || value.length() > NAME_FILTER_MAX
                || !NAME_FILTER.matcher(value).matches()) {
            issues.add(ProblemIssue.schemaViolation(
                    "query", "name", "'name' must be 1 to 64 letters or digits.", shorten(value)));
            return null;
        }
        return value;
    }

    /**
     * A date filter (D20, D110, D130): a date matches its whole UTC day, a date-time its second. A
     * date-time without an offset is UTC.
     */
    public static TimeRange dateFilter(String value, String field, List<ProblemIssue> issues) {
        if (DATE.matcher(value).matches()) {
            try {
                if (value.length() == 10) {
                    LocalDate day = LocalDate.parse(value, STRICT_DATE);
                    Instant from = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                    return new TimeRange(from, from.plus(1, ChronoUnit.DAYS));
                }
                LocalDate.parse(value.substring(0, 10), STRICT_DATE);
                Instant at = parseDateTime(value).truncatedTo(ChronoUnit.SECONDS);
                return new TimeRange(at, at.plusSeconds(1));
            } catch (DateTimeException | ArithmeticException e) {
                // falls through to the issue below
            }
        }
        issues.add(ProblemIssue.schemaViolation(
                "query", field, "'" + field + "' must be a valid date (YYYY-MM-DD) or date-time.", shorten(value)));
        return null;
    }

    private static Instant parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant();
        } catch (DateTimeException e) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC);
        }
    }

    /** An absolute URI (RFC 3986, with a scheme) as sent (D73). */
    public static boolean absoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > 2048) {
            return false;
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() && uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    static String shorten(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}

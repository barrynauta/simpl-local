package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.VocabularyNames;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.jena.rfc3986.IRI3986;

/**
 * Field and parameter rules of the decided contract (D19, D20, D22, D37, D49, D56, D73, D91, D112, D130). Each
 * method returns the checked value or throws 400 with one issue.
 */
public final class RequestRules {

    public static final int MAX_TEXT = 1000;
    public static final int MAX_INT = Integer.MAX_VALUE;

    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");

    private RequestRules() {}

    /** A vocabulary name in a path (D49). */
    public static String vocabularyName(String raw) {
        return pathName(raw, "vocabularyName");
    }

    /** An external vocabulary name in a path (D49). */
    public static String externalName(String raw) {
        return pathName(raw, "name");
    }

    private static String pathName(String raw, String parameter) {
        String value = raw == null ? null : QueryParameters.decode(raw);
        if (!VocabularyNames.isValid(value)) {
            throw ApiException.badRequest(
                    "path",
                    parameter,
                    "Path parameter '" + parameter + "' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$.");
        }
        return value;
    }

    /** A version number in a path: digits only, 1 to 2147483647 (D49, D91, D112). */
    public static long version(String raw) {
        String value = raw == null ? null : QueryParameters.decode(raw);
        Long number = integer(value, 1, MAX_INT);
        if (number == null) {
            throw ApiException.badRequest(
                    "path", "version", "Path parameter 'version' must be an integer from 1 to 2147483647.");
        }
        return number;
    }

    /**
     * An integer query parameter (D22, D91, D112).
     *
     * @param name parameter name
     * @param value the value or null
     * @param fallback the default
     * @param max the maximum
     * @return the value
     */
    public static int queryInteger(String name, String value, int fallback, int max) {
        if (value == null) {
            return fallback;
        }
        Long number = integer(value, 1, max);
        if (number == null) {
            throw ApiException.badRequest(
                    "query", name, "Query parameter '" + name + "' must be an integer from 1 to " + max + ".");
        }
        return number.intValue();
    }

    private static Long integer(String value, long min, long max) {
        if (value == null || !DIGITS.matcher(value).matches() || value.length() > 12) {
            return null;
        }
        long number = Long.parseLong(value);
        return number < min || number > max ? null : number;
    }

    /** The name filter of D19. */
    public static String nameFilter(String value) {
        if (value == null) {
            return null;
        }
        if (!NAME_FILTER.matcher(value).matches()) {
            throw ApiException.badRequest(
                    "query", "name", "Query parameter 'name' must have 1 to 64 letters or digits.");
        }
        return value;
    }

    /**
     * A date filter (D20, D110, D130): a date covers the whole UTC day; a date-time matches to the second.
     *
     * @param name parameter name
     * @param value the value or null
     * @return lower bound (inclusive) and upper bound (exclusive), or null
     */
    public static Instant[] dateFilter(String name, String value) {
        if (value == null) {
            return null;
        }
        String message = "Query parameter '" + name + "' must be a date (YYYY-MM-DD) or a date-time.";
        if (!DATE_FILTER.matcher(value).matches()) {
            throw ApiException.badRequest("query", name, message);
        }
        try {
            if (value.length() == 10) {
                Instant start =
                        LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
                return new Instant[] {start, start.plus(1, ChronoUnit.DAYS)};
            }
            Instant at = parseDateTime(value).truncatedTo(ChronoUnit.SECONDS);
            return new Instant[] {at, at.plusSeconds(1)};
        } catch (DateTimeException e) {
            throw ApiException.badRequest("query", name, message);
        }
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

    /**
     * A required text field of 1 to 1000 characters after trimming (D37).
     *
     * @param field field name
     * @param value the value or null
     * @param issues collects the issue
     * @return the trimmed value or null
     */
    public static String requiredText(String field, String value, List<Issue> issues) {
        if (value == null) {
            issues.add(Issue.schemaViolation("body", field, "Field '" + field + "' is required."));
            return null;
        }
        String trimmed = value.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < 1 || length > MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    "body", field, "Field '" + field + "' must have 1 to 1000 characters after trimming."));
            return null;
        }
        return trimmed;
    }

    /**
     * An optional text field of at most 1000 characters; blank means absent (D56, D148).
     *
     * @return the trimmed value or null
     */
    public static String optionalText(String field, String value, List<Issue> issues) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    "body", field, "Field '" + field + "' must have at most 1000 characters after trimming."));
            return null;
        }
        return trimmed;
    }

    /** A required name field (D37, D56). */
    public static String nameField(String value, List<Issue> issues) {
        if (value == null) {
            issues.add(Issue.schemaViolation("body", "name", "Field 'name' is required."));
            return null;
        }
        if (!VocabularyNames.isValid(value)) {
            issues.add(Issue.schemaViolation(
                    "body", "name", "Field 'name' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
            return null;
        }
        return value;
    }

    /** True when the text is an absolute URI by RFC 3986 (D73). */
    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > 4096) {
            return false;
        }
        try {
            IRI3986 iri = IRI3986.create(value);
            return iri.hasScheme();
        } catch (RuntimeException e) {
            return false;
        }
    }
}

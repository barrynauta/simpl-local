package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Field and value rules of the decided contract (D19, D20, D37, D49, D56, D73, D91, D112). */
public final class Validation {

    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    public static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern DATE_PREFIX = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    private static final Pattern OFFSET = Pattern.compile("([+-])(\\d{2}):(\\d{2})$");
    public static final int TEXT_MAX = 1000;

    private Validation() {}

    /** Vocabulary or external vocabulary name: 3 to 64 characters and the pattern of D37. */
    public static boolean validName(String s) {
        return s != null
                && s.length() >= 3
                && s.length() <= 64
                && NAME.matcher(s).matches();
    }

    /** Digits only, 1 to 2147483647 (D91, D112); null when invalid. */
    public static Integer positiveInt(String s) {
        if (s == null || s.isEmpty() || s.length() > 10) {
            return null;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        long v = Long.parseLong(s);
        return v >= 1 && v <= Integer.MAX_VALUE ? (int) v : null;
    }

    /** Path name check (D49): 400 schemaViolation in path. */
    public static void pathName(String paramName, String value) {
        if (!validName(value)) {
            throw ApiException.badRequest(Issue.schemaViolation(
                    "path",
                    paramName,
                    "Path parameter '" + paramName + "' must be 3 to 64 characters matching ^[A-Z][a-zA-Z0-9]*$.",
                    value));
        }
    }

    /** Path version check (D49, D91). */
    public static int pathVersion(String value) {
        Integer v = positiveInt(value);
        if (v == null) {
            throw ApiException.badRequest(Issue.schemaViolation(
                    "path", "version", "Path parameter 'version' must be an integer from 1 to 2147483647.", value));
        }
        return v;
    }

    /** Text after trimming must be 1 to 1000 characters (D37). */
    public static boolean validText(String s) {
        if (s == null) {
            return false;
        }
        String t = s.strip();
        return !t.isEmpty() && t.length() <= TEXT_MAX;
    }

    /** Absolute URI with a scheme, RFC 3986 (D73). */
    public static boolean absoluteUri(String s) {
        if (s == null || s.isEmpty() || s.length() > 2048) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= ' ' || c == 0x7f) {
                return false;
            }
        }
        try {
            URI u = new URI(s);
            return u.isAbsolute()
                    && u.getScheme() != null
                    && !u.getRawSchemeSpecificPart().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * A date filter (D20, D110, D130): a date matches the whole UTC day, a date-time the second.
     *
     * @return [from, to) or null when invalid
     */
    public static Instant[] dateRange(String s) {
        if (s == null || !DATE_PREFIX.matcher(s).matches()) {
            return null;
        }
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                Instant from = d.atStartOfDay().toInstant(ZoneOffset.UTC);
                return new Instant[] {from, from.plus(1, ChronoUnit.DAYS)};
            }
            Instant t;
            String upper = s.toUpperCase(java.util.Locale.ROOT);
            Matcher m = OFFSET.matcher(upper);
            if (upper.endsWith("Z")) {
                t = OffsetDateTime.parse(upper, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .toInstant();
            } else if (m.find() && upper.indexOf('T') < m.start()) {
                int hours = Integer.parseInt(m.group(2));
                int minutes = Integer.parseInt(m.group(3));
                if (hours > 18 || minutes > 59 || hours == 18 && minutes > 0) {
                    return null;
                }
                t = OffsetDateTime.parse(upper, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .toInstant();
            } else {
                t = LocalDateTime.parse(upper, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .toInstant(ZoneOffset.UTC);
            }
            Instant from = t.truncatedTo(ChronoUnit.SECONDS);
            return new Instant[] {from, from.plusSeconds(1)};
        } catch (DateTimeException | ArithmeticException e) {
            return null;
        }
    }
}

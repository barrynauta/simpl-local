package eu.simpl.vocabularymanager.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A creationDate or lastUpdated filter (decision D20): a date matches every value on that UTC day; a date-time matches
 * values equal to it, to the second.
 */
public final class DateFilter {

    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE_TIME = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.*$");

    private final LocalDate day;
    private final Instant second;

    private DateFilter(LocalDate day, Instant second) {
        this.day = day;
        this.second = second;
    }

    /** Parses the query value; empty when it is not a valid date or date-time. */
    public static Optional<DateFilter> parse(String value) {
        try {
            if (DATE.matcher(value).matches()) {
                return Optional.of(new DateFilter(LocalDate.parse(value), null));
            }
            if (DATE_TIME.matcher(value).matches()) {
                return Optional.of(new DateFilter(null, parseDateTime(value).truncatedTo(ChronoUnit.SECONDS)));
            }
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Instant parseDateTime(String value) {
        // A '+' in an unencoded query string arrives as a space.
        String text = value.replace(' ', '+');
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        }
    }

    public boolean matches(Instant value) {
        if (value == null) {
            return false;
        }
        if (day != null) {
            return value.atZone(ZoneOffset.UTC).toLocalDate().equals(day);
        }
        return value.truncatedTo(ChronoUnit.SECONDS).equals(second);
    }
}

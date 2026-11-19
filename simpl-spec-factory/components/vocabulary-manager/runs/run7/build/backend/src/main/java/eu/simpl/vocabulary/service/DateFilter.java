package eu.simpl.vocabulary.service;

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
 * Date filter of D20 and D58: a date matches every value on that UTC day; a date-time matches values equal
 * to it, to the second.
 */
public final class DateFilter {

    private static final Pattern SHAPE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");

    private final LocalDate day;
    private final Instant second;

    private DateFilter(LocalDate day, Instant second) {
        this.day = day;
        this.second = second;
    }

    /**
     * Parses a filter value.
     *
     * @param value the value
     * @return the filter, empty when the value is not a valid date or date-time
     */
    public static Optional<DateFilter> parse(String value) {
        if (value == null || !SHAPE.matcher(value).matches()) {
            return Optional.empty();
        }
        try {
            if (value.length() == 10) {
                return Optional.of(new DateFilter(LocalDate.parse(value), null));
            }
            return Optional.of(new DateFilter(null, parseDateTime(value).truncatedTo(ChronoUnit.SECONDS)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Instant parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        }
    }

    /**
     * Tells whether a stored time matches.
     *
     * @param value the stored time (UTC, to the second)
     * @return true when it matches
     */
    public boolean matches(Instant value) {
        if (day != null) {
            return LocalDate.ofInstant(value, ZoneOffset.UTC).equals(day);
        }
        return value.truncatedTo(ChronoUnit.SECONDS).equals(second);
    }
}

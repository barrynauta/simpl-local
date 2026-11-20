package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.store.ListQuery;
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
 * Date filters of D20, D110 and D130: a date matches its whole UTC day, a date-time its second; a
 * date-time without offset is UTC; offsets beyond plus or minus 18:00 are invalid.
 */
public final class DateFilters {

    private static final Pattern SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(T.*)?");

    private DateFilters() {}

    /** The UTC range for the value, or empty when the value is not a valid date or date-time. */
    public static Optional<ListQuery.TimeRange> parse(String value) {
        if (value == null || value.length() > 40 || !SHAPE.matcher(value).matches()) {
            return Optional.empty();
        }
        try {
            if (value.length() == 10) {
                Instant start =
                        LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
                return Optional.of(new ListQuery.TimeRange(start, start.plus(1, ChronoUnit.DAYS)));
            }
            Instant at = parseDateTime(value).truncatedTo(ChronoUnit.SECONDS);
            return Optional.of(new ListQuery.TimeRange(at, at.plusSeconds(1)));
        } catch (DateTimeParseException | ArithmeticException ex) {
            return Optional.empty();
        }
    }

    private static Instant parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        }
    }
}

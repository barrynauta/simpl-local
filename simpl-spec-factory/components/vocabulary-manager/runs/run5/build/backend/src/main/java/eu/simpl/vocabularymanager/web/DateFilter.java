package eu.simpl.vocabularymanager.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

/**
 * creationDate and lastUpdated filter (D20): a date matches every value on that UTC day; a date-time matches values
 * equal to it, to the second.
 */
public final class DateFilter {

    private static final Pattern SHAPE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ISO_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);

    private final LocalDate day;
    private final Instant moment;

    private DateFilter(LocalDate day, Instant moment) {
        this.day = day;
        this.moment = moment;
    }

    /**
     * Parses a filter value.
     *
     * @param value the query value
     * @return the filter, or null when the value is not a valid date or date-time
     */
    public static DateFilter parse(String value) {
        if (value == null || !SHAPE.matcher(value).matches()) {
            return null;
        }
        try {
            if (value.length() == 10) {
                return new DateFilter(
                        LocalDate.parse(
                                value, DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)),
                        null);
            }
            TemporalAccessor parsed = DATE_TIME.parse(value);
            Instant instant = parsed.isSupported(ChronoField.OFFSET_SECONDS)
                    ? OffsetDateTime.from(parsed).toInstant()
                    : LocalDateTime.from(parsed).toInstant(ZoneOffset.UTC);
            return new DateFilter(null, instant.truncatedTo(ChronoUnit.SECONDS));
        } catch (java.time.DateTimeException e) {
            return null;
        }
    }

    /** True when the stored time (ISO 8601, UTC) matches. */
    public boolean matches(String stored) {
        if (stored == null) {
            return false;
        }
        Instant value = Instant.parse(stored).truncatedTo(ChronoUnit.SECONDS);
        if (day != null) {
            return value.atZone(ZoneOffset.UTC).toLocalDate().equals(day);
        }
        return value.equals(moment);
    }
}

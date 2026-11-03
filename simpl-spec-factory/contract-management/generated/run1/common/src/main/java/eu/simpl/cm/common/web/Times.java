package eu.simpl.cm.common.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** D15 date formats. */
public final class Times {
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private Times() {
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** OpenAPI-documented responses: UTC local date-time without offset. */
    public static String local(Instant i) {
        return i == null ? null : LOCAL.format(i);
    }

    /** Added operations, events, VCs: RFC 3339 with Z. */
    public static String rfc3339(Instant i) {
        return i == null ? null : i.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    /** VC dates without fraction, as in the worked vector. */
    public static String vc(Instant i) {
        return i == null ? null : i.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public static String file(Instant i) {
        return i == null ? null : FILE.format(i);
    }

    /** Lenient parser: RFC 3339 with offset, or local date-time taken as UTC. */
    public static Instant parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(s).toInstant();
        } catch (Exception e) {
            return java.time.LocalDateTime.parse(s).toInstant(ZoneOffset.UTC);
        }
    }
}

package eu.simpl.cm.manager.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** D15 and D34 date formats. */
public final class Dates {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private Dates() {
    }

    /** OpenAPI-documented responses: UTC local date-time without offset. */
    public static String local(Instant i) {
        return i == null ? null : LOCAL.format(i);
    }

    public static String file(Instant i) {
        return i == null ? "" : FILE.format(i);
    }

    /** Added operations, events and VCs: RFC 3339 UTC with Z. */
    public static String rfc3339(Instant i) {
        return i == null ? null : i.toString();
    }
}

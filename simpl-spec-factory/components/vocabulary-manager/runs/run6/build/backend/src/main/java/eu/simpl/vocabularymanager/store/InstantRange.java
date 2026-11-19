package eu.simpl.vocabularymanager.store;

import java.time.Instant;

/**
 * A half-open time range [from, to) used by the date filters (decision D20).
 *
 * @param from first instant included
 * @param to first instant excluded
 */
public record InstantRange(Instant from, Instant to) {}

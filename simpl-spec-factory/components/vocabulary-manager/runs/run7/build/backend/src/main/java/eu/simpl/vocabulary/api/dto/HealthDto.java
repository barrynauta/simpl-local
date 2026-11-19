package eu.simpl.vocabulary.api.dto;

/**
 * Health response (D44).
 *
 * @param status UP, DEGRADED or DOWN
 */
public record HealthDto(String status) {}

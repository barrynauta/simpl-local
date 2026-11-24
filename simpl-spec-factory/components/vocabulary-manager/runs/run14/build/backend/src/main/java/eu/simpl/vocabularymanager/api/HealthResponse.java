package eu.simpl.vocabularymanager.api;

/**
 * Body of GET /health (D44).
 *
 * @param status UP, DEGRADED or DOWN
 */
public record HealthResponse(String status) {}

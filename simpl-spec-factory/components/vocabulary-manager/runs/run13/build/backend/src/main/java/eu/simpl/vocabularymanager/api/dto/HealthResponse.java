package eu.simpl.vocabularymanager.api.dto;

/** Wire form of GET /health (UP, DEGRADED, DOWN). */
public record HealthResponse(String status) {}

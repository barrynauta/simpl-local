package eu.simpl.schemamanager.domain;

import java.util.List;
import java.util.UUID;

/** A webhook subscription (D41). */
public record Webhook(UUID webhookId, String targetUrl, List<String> events) {
}

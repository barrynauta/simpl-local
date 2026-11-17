package eu.simpl.schemamanager.notify;

import eu.simpl.schemamanager.domain.Concept;
import java.time.Instant;

/**
 * A committed lifecycle change. Published inside the transaction and handled after commit,
 * asynchronously, by the e-mail and webhook notifiers (D39 to D44).
 *
 * @param kind      PUBLISHED (create or PATCH to PUBLISHED), NEW_VERSION_PUBLISHED, REVOKED (PATCH)
 * @param concept   the concept after the change
 * @param version   the highest version after the change
 * @param changelog changelog of that version
 * @param schemaUri absolute versioned URI of that version
 */
public record SchemaEvent(Kind kind, Concept concept, int version, String changelog, String schemaUri,
                          Instant occurredAt) {

    public enum Kind { PUBLISHED, NEW_VERSION_PUBLISHED, REVOKED }

    public String webhookEventType() {
        return kind == Kind.REVOKED ? "SchemaRevoked" : "SchemaPublished";
    }
}

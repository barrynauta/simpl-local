package eu.simpl.schemamanager.domain;

import java.time.Instant;
import java.util.UUID;

/** One stored version of a concept. Content is kept byte for byte (D58). */
public record SchemaVersion(
        UUID conceptId,
        int version,
        String changelog,
        Instant created,
        String createdBy) {
}

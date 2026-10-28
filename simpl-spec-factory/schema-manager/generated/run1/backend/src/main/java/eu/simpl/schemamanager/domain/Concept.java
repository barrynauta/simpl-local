package eu.simpl.schemamanager.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A schema concept, identified by (schemaType, name) (D11).
 * latestVersion and latestChangelog are filled by queries that join the highest version.
 */
public record Concept(
        UUID id,
        SchemaType schemaType,
        String name,
        String title,
        String description,
        String resourceType,
        SchemaStatus status,
        Instant created,
        Instant modified,
        String createdBy,
        Integer latestVersion,
        String latestChangelog) {

    /** Slot key: the set of concepts that compete for publication (D16). */
    public String slotKey() {
        return slotKey(schemaType, resourceType);
    }

    public static String slotKey(SchemaType type, String resourceType) {
        return type == SchemaType.SEARCH ? "SEARCH" : "RESOURCE:" + resourceType;
    }

    public Concept withStatus(SchemaStatus newStatus, Instant newModified) {
        return new Concept(id, schemaType, name, title, description, resourceType, newStatus, created,
                newModified, createdBy, latestVersion, latestChangelog);
    }

    public Concept withLatest(int version, String changelog) {
        return new Concept(id, schemaType, name, title, description, resourceType, status, created,
                modified, createdBy, version, changelog);
    }
}

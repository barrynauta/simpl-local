package eu.simpl.vocabularymanager.store;

import java.time.Instant;

/**
 * A version to store.
 *
 * @param name vocabulary name
 * @param version version number
 * @param description trimmed description
 * @param changelog trimmed changelog
 * @param createdBy creator (D27)
 * @param now creation time, to the second (D58)
 * @param content content bytes as uploaded (D29)
 * @param contentSha256 hex SHA-256 of the content (D46)
 */
public record NewVersion(
        String name,
        int version,
        String description,
        String changelog,
        String createdBy,
        Instant now,
        byte[] content,
        String contentSha256) {}

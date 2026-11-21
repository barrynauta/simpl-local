package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/** The domain model (docs/logical-data-model.md, traced to the CDM of decision D2). */
public final class Model {

    private Model() {}

    /** Lifecycle status of a vocabulary or a vocabulary version (D3, glossary state tables). */
    public enum Status {
        ACTIVE,
        DEPRECATED;

        public static Status parse(String s) {
            for (Status v : values()) {
                if (v.name().equals(s)) {
                    return v;
                }
            }
            return null;
        }
    }

    /**
     * A vocabulary (CDM InternalVocabulary) with values of its latest version (D25).
     *
     * @param name identity (D30, D37)
     * @param status vocabulary status (D3)
     * @param latestVersion highest version number (D5)
     * @param description description of the latest version (D28)
     * @param createdBy creator of version 1 (D27)
     * @param creationDate creation time of version 1
     * @param lastUpdateDate last change of the vocabulary or any version (D25, D160)
     */
    public record Vocabulary(
            String name,
            Status status,
            int latestVersion,
            String description,
            String createdBy,
            Instant creationDate,
            Instant lastUpdateDate) {}

    /**
     * A vocabulary version (CDM InternalVocabularyVersion, D26).
     *
     * @param name vocabulary name
     * @param version version number (D7)
     * @param description description of this version (D28)
     * @param changelog what changed (D31)
     * @param status version status (D3, D4)
     * @param createdBy creator (D27)
     * @param creationDate creation time
     * @param lastUpdateDate last status change of this version (D4, D160)
     * @param latestVersion latest version number of the vocabulary (D5)
     */
    public record VocabularyVersion(
            String name,
            int version,
            String description,
            String changelog,
            Status status,
            String createdBy,
            Instant creationDate,
            Instant lastUpdateDate,
            int latestVersion) {}

    /**
     * A registered external vocabulary (CDM ExternalVocabulary, D30, D56, D73).
     *
     * @param name identity
     * @param namespaceUri namespace URI as sent
     * @param aliasUris alias URIs as sent, in the order sent
     * @param sourceUri optional source
     * @param description optional description (D148)
     * @param createdBy creator (D27)
     * @param creationDate registration time
     */
    public record ExternalVocabulary(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            Instant creationDate) {}

    /**
     * One page of a list.
     *
     * @param items the rows
     * @param total number of rows over all pages
     * @param <T> row type
     */
    public record Page<T>(List<T> items, long total) {}

    /**
     * A vocabulary validation warning (D10); not stored.
     *
     * @param code warning code (closed set)
     * @param description text
     * @param location where
     */
    public record Warning(String code, String description, String location) {}

    /**
     * Stored content of a version: hash and size for the identical-version check (D46).
     *
     * @param sha256 hex SHA-256 of the bytes
     * @param size number of bytes
     * @param description description of the version
     */
    public record ContentFingerprint(String sha256, long size, String description) {}
}

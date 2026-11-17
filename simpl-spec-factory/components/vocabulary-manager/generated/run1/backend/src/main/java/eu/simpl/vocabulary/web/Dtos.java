package eu.simpl.vocabulary.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.validation.ValidationWarning;
import java.time.Instant;
import java.util.List;

/** Response bodies, shaped as the OpenAPI schemas. */
public final class Dtos {

    private Dtos() {}

    /** OpenAPI VocabularyCategory: latest metadata of a vocabulary. */
    public record VocabularyCategory(String href, String contentHref, String name, int version, String description,
                                     Status status, String createdBy, Instant creationDate, Instant lastUpdateDate) {}

    /**
     * OpenAPI VocabularyMetadata: metadata of one version. {@code createdBy} is an addition (SIMPL-18176 AC3 asks
     * for the creator of a version); it is not in the OpenAPI schema.
     */
    public record VocabularyMetadata(String href, String contentHref, String latestVersionHref, String name,
                                     int version, String description, String changelog, Status status,
                                     String createdBy, Instant creationDate, Instant lastUpdateDate) {}

    /** OpenAPI VocabularyVersionMetadataResponse, returned by the two create operations. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record VocabularyVersionMetadataResponse(String href, String contentHref, String name, int version,
                                                    String description, String changelog, Status status,
                                                    String createdBy, Instant creationDate, Instant lastUpdateDate,
                                                    List<ValidationWarning> validationWarnings) {}

    /** OpenAPI VocabularySearchItem. */
    public record VocabularySearchItem(String href, String contentHref, String name, int version, Status status,
                                       boolean latest) {}

    /** OpenAPI ExternalVocabulary. */
    public record ExternalVocabularyDto(String href, String name, String namespaceUri, List<String> aliasUris,
                                        String sourceUri, String description, String createdBy,
                                        Instant creationDate) {}

    /** OpenAPI HealthResponse. */
    public record HealthResponse(String status) {}

    /** OpenAPI CollectionPage with items. */
    public record PageResponse<T>(int page, int totalPages, long total, int pageSize, String self, String first,
                                  String prev, String next, String last, List<T> items) {}
}

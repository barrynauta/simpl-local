package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import java.util.List;

/** Wire representations of the decided contract (docs/openapi-decided.yaml). Field order follows the OpenAPI. */
public final class Dtos {

    private Dtos() {}

    /** The vocabulary (wire schema VocabularyCategory, D25). */
    public record VocabularyCategory(
            String href,
            String contentHref,
            String name,
            int version,
            String description,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {}

    /** The vocabulary version (wire schema VocabularyMetadata, D26, D27, D5). */
    public record VocabularyMetadata(
            String href,
            String contentHref,
            String latestVersionHref,
            String name,
            int version,
            String description,
            String changelog,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {}

    /** The created version plus its validation warnings (VocabularyVersionMetadataResponse, D10). */
    public record VocabularyVersionMetadataResponse(
            String href,
            String contentHref,
            String latestVersionHref,
            String name,
            int version,
            String description,
            String changelog,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate,
            List<ValidationWarning> validationWarnings) {}

    /** The external vocabulary; aliasUris is always present (D110). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExternalVocabulary(
            String href,
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            String creationDate) {}

    /** One internal search result (VocabularySearchItem, D38). */
    public record VocabularySearchItem(
            String href, String contentHref, String name, int version, String status, boolean latest) {}

    /** A collection page (CollectionPage); absent links are left out. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CollectionPage<T>(
            int page,
            int totalPages,
            long total,
            int pageSize,
            String self,
            String first,
            String prev,
            String next,
            String last,
            List<T> items) {}

    /** The health status (HealthResponse). */
    public record HealthResponse(String status) {}
}

package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.time.Instant;
import java.util.List;

/** Wire schemas of the OpenAPI (names kept, decisions D25, D26, D39). */
public final class ApiModel {

    private ApiModel() {}

    /** VocabularyCategory: the vocabulary (decision D25). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
        "href",
        "contentHref",
        "name",
        "version",
        "description",
        "status",
        "createdBy",
        "creationDate",
        "lastUpdateDate"
    })
    public record VocabularyCategory(
            String href,
            String contentHref,
            String name,
            int version,
            String description,
            VocabularyStatus status,
            String createdBy,
            Instant creationDate,
            Instant lastUpdateDate) {}

    /** VocabularyMetadata: the vocabulary version (decisions D5, D26, D27). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
        "href",
        "contentHref",
        "latestVersionHref",
        "name",
        "version",
        "description",
        "changelog",
        "status",
        "createdBy",
        "creationDate",
        "lastUpdateDate"
    })
    public record VocabularyMetadata(
            String href,
            String contentHref,
            String latestVersionHref,
            String name,
            int version,
            String description,
            String changelog,
            VocabularyStatus status,
            String createdBy,
            Instant creationDate,
            Instant lastUpdateDate) {}

    /** VocabularyVersionMetadataResponse: the created version plus validationWarnings (decision D10). */
    @JsonPropertyOrder({
        "href",
        "contentHref",
        "latestVersionHref",
        "name",
        "version",
        "description",
        "changelog",
        "status",
        "createdBy",
        "creationDate",
        "lastUpdateDate",
        "validationWarnings"
    })
    public record VocabularyVersionMetadataResponse(
            String href,
            String contentHref,
            String latestVersionHref,
            String name,
            int version,
            String description,
            String changelog,
            VocabularyStatus status,
            String createdBy,
            Instant creationDate,
            Instant lastUpdateDate,
            List<ValidationWarning> validationWarnings) {}

    /** VocabularySearchItem (decision D38). */
    @JsonPropertyOrder({"href", "contentHref", "name", "version", "status", "latest"})
    public record VocabularySearchItem(
            String href, String contentHref, String name, int version, VocabularyStatus status, boolean latest) {}

    /** ExternalVocabulary (decision D41). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
        "href",
        "name",
        "namespaceUri",
        "aliasUris",
        "sourceUri",
        "description",
        "createdBy",
        "creationDate"
    })
    public record ExternalVocabularyResponse(
            String href,
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            Instant creationDate) {}

    /** CollectionPage with items; absent links are left out (decision D22). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"page", "totalPages", "total", "pageSize", "self", "first", "prev", "next", "last", "items"})
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

    /** HealthResponse. */
    public record HealthResponse(String status) {}
}

package eu.simpl.vocabularymanager.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Wire representations of the OpenAPI schemas, with the decided changes of D79 (createdBy on versions, validationWarnings
 * always present, latestVersionHref always filled). Wire names stay as in the OpenAPI (D25, D26).
 */
public final class Representations {

    private Representations() {}

    private static String time(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /**
     * VocabularyCategory: the vocabulary (D25).
     *
     * @param href relative URL
     * @param contentHref content URL of the latest version
     * @param name name
     * @param version latest version number
     * @param description latest version description
     * @param status vocabulary status
     * @param createdBy creator of version 1
     * @param creationDate creation time of version 1
     * @param lastUpdateDate last change
     */
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
            long version,
            String description,
            VocabularyStatus status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {

        /**
         * @param vocabulary the vocabulary
         * @param contentBase contentBase of E2
         * @return the representation
         */
        public static VocabularyCategory of(Vocabulary vocabulary, String contentBase) {
            return new VocabularyCategory(
                    "/vocabularies/" + vocabulary.name(),
                    contentBase + "/content/" + vocabulary.name(),
                    vocabulary.name(),
                    vocabulary.latestVersion(),
                    vocabulary.latestDescription(),
                    vocabulary.status(),
                    vocabulary.createdBy(),
                    time(vocabulary.creationDate()),
                    time(vocabulary.lastUpdateDate()));
        }
    }

    /**
     * VocabularyMetadata: the vocabulary version (D26), with createdBy (D27).
     *
     * @param href relative URL
     * @param contentHref content URL
     * @param latestVersionHref href of the latest version (D5)
     * @param name vocabulary name
     * @param version version number
     * @param description description
     * @param changelog changelog
     * @param status version status
     * @param createdBy creator
     * @param creationDate creation time
     * @param lastUpdateDate last status change
     */
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
            long version,
            String description,
            String changelog,
            VocabularyStatus status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {

        /**
         * @param version the version
         * @param contentBase contentBase of E2
         * @return the representation
         */
        public static VocabularyMetadata of(VocabularyVersion version, String contentBase) {
            String base = "/vocabularies/" + version.name() + "/versions/";
            return new VocabularyMetadata(
                    base + version.version(),
                    contentBase + "/content/" + version.name() + "/versions/" + version.version(),
                    base + version.latestVersion(),
                    version.name(),
                    version.version(),
                    version.description(),
                    version.changelog(),
                    version.status(),
                    version.createdBy(),
                    time(version.creationDate()),
                    time(version.lastUpdateDate()));
        }
    }

    /**
     * VocabularyVersionMetadataResponse: the created version plus validationWarnings (D10, D26).
     *
     * @param href relative URL
     * @param contentHref content URL
     * @param latestVersionHref href of the latest version (the version itself)
     * @param name vocabulary name
     * @param version version number
     * @param description description
     * @param changelog changelog
     * @param status version status
     * @param createdBy creator
     * @param creationDate creation time
     * @param lastUpdateDate creation time
     * @param validationWarnings warnings, empty when none
     */
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
            long version,
            String description,
            String changelog,
            VocabularyStatus status,
            String createdBy,
            String creationDate,
            String lastUpdateDate,
            List<ValidationWarning> validationWarnings) {

        /**
         * @param version the created version
         * @param warnings the validation warnings
         * @param contentBase contentBase of E2
         * @return the representation
         */
        public static VocabularyVersionMetadataResponse of(
                VocabularyVersion version, List<ValidationWarning> warnings, String contentBase) {
            VocabularyMetadata metadata = VocabularyMetadata.of(version, contentBase);
            return new VocabularyVersionMetadataResponse(
                    metadata.href(),
                    metadata.contentHref(),
                    metadata.latestVersionHref(),
                    metadata.name(),
                    metadata.version(),
                    metadata.description(),
                    metadata.changelog(),
                    metadata.status(),
                    metadata.createdBy(),
                    metadata.creationDate(),
                    metadata.lastUpdateDate(),
                    List.copyOf(warnings));
        }
    }

    /**
     * VocabularySearchItem (D38).
     *
     * @param href relative URL of the vocabulary (latest) or the version
     * @param contentHref content URL
     * @param name name
     * @param version version number
     * @param status vocabulary status (latest) or version status
     * @param latest true when resolved as latest
     */
    @JsonPropertyOrder({"href", "contentHref", "name", "version", "status", "latest"})
    public record VocabularySearchItem(
            String href, String contentHref, String name, long version, VocabularyStatus status, boolean latest) {}

    /**
     * ExternalVocabulary.
     *
     * @param href relative URL
     * @param name name
     * @param namespaceUri namespace URI as sent
     * @param aliasUris alias URIs as sent
     * @param sourceUri source URI, or null
     * @param description description, or null
     * @param createdBy creator
     * @param creationDate registration time
     */
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
    public record ExternalVocabularyRepresentation(
            String href,
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            String creationDate) {

        /**
         * @param vocabulary the external vocabulary
         * @return the representation
         */
        public static ExternalVocabularyRepresentation of(ExternalVocabulary vocabulary) {
            return new ExternalVocabularyRepresentation(
                    "/external-vocabularies/" + vocabulary.name(),
                    vocabulary.name(),
                    vocabulary.namespaceUri(),
                    vocabulary.aliasUris(),
                    vocabulary.sourceUri(),
                    vocabulary.description(),
                    vocabulary.createdBy(),
                    time(vocabulary.creationDate()));
        }
    }

    /**
     * CollectionPage with items (D22, D38, D59, D78).
     *
     * @param page page number
     * @param totalPages number of pages
     * @param total number of items
     * @param pageSize page size
     * @param self link to this page, or null for searches
     * @param first link to the first page, or null for searches
     * @param prev link to the previous page, or null
     * @param next link to the next page, or null
     * @param last link to the last page, or null
     * @param items the items
     * @param <T> item type
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"page", "totalPages", "total", "pageSize", "self", "first", "prev", "next", "last", "items"})
    public record CollectionPage<T>(
            long page,
            long totalPages,
            long total,
            int pageSize,
            String self,
            String first,
            String prev,
            String next,
            String last,
            List<T> items) {}

    /**
     * HealthResponse.
     *
     * @param status UP, DEGRADED or DOWN
     */
    public record HealthResponse(String status) {}
}

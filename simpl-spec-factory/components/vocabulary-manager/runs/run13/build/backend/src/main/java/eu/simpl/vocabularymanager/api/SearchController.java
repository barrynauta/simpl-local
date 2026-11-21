package eu.simpl.vocabularymanager.api;

import static eu.simpl.vocabularymanager.api.ApiSupport.JSON;
import static eu.simpl.vocabularymanager.api.ApiSupport.PROBLEM_JSON;

import eu.simpl.vocabularymanager.api.dto.CollectionResponse;
import eu.simpl.vocabularymanager.api.dto.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.api.dto.SearchItemResponse;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.ReferenceState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** The read-only search operations (D38, D66, D68, D137, D153). */
@RestController
public class SearchController {

    static final int MAX_REFERENCES = 100;
    private static final Set<String> EXTERNAL_SEARCH_FIELDS =
            Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");

    private final VocabularyService vocabularies;
    private final ExternalVocabularyService externals;
    private final Representations representations;

    public SearchController(
            VocabularyService vocabularies, ExternalVocabularyService externals, Representations representations) {
        this.vocabularies = vocabularies;
        this.externals = externals;
        this.representations = representations;
    }

    /** One requested reference: a name, with a version or for the latest version. */
    record SearchReference(String name, Integer version) {}

    @PostMapping(
            path = "/v1/vocabularies/searches",
            consumes = JSON,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<CollectionResponse<SearchItemResponse>> searchVocabularies(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        List<SearchReference> references = searchReferences(body, issues);
        ApiSupport.failOnIssues(issues);

        Set<String> names = new LinkedHashSet<>();
        references.forEach(reference -> names.add(reference.name()));
        ReferenceState state = vocabularies.searchState(names);
        List<SearchItemResponse> items = new ArrayList<>();
        Set<SearchReference> seen = new HashSet<>();
        for (SearchReference reference : references) {
            if (!seen.add(reference)) {
                continue;
            }
            ReferenceState.InternalState vocabulary = state.internal().get(reference.name());
            if (vocabulary == null) {
                continue;
            }
            if (reference.version() == null) {
                items.add(
                        representations.latestItem(vocabulary.name(), vocabulary.latestVersion(), vocabulary.status()));
            } else {
                VocabularyStatus status = vocabulary.versions().get(reference.version());
                if (status != null) {
                    items.add(representations.versionItem(vocabulary.name(), reference.version(), status));
                }
            }
        }
        return ApiSupport.json(Representations.unpaged(items));
    }

    private static List<SearchReference> searchReferences(JsonNode body, List<ProblemIssue> issues) {
        JsonBody.unknownProperties(body, Set.of("items"), "", issues);
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > MAX_REFERENCES) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "items", "'items' must be an array of 1 to 100 references.", null));
            return List.of();
        }
        List<SearchReference> references = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            String prefix = "items[" + index + "]";
            JsonNode item = items.get(index);
            if (!item.isObject()) {
                issues.add(ProblemIssue.schemaViolation("body", prefix, "'" + prefix + "' must be an object.", null));
                continue;
            }
            JsonBody.unknownProperties(item, Set.of("name", "version"), prefix + ".", issues);
            JsonNode name = item.get("name");
            String nameValue = name != null && name.isString() ? name.asString() : null;
            String checked = RequestRules.name(nameValue, "body", prefix + ".name", issues);
            Integer version = null;
            if (item.has("version")) {
                version = JsonBody.positiveInt(item.get("version"), prefix + ".version", issues);
            }
            if (checked != null) {
                references.add(new SearchReference(checked, version));
            }
        }
        return references;
    }

    @PostMapping(
            path = "/v1/external-vocabulary/searches",
            consumes = JSON,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<CollectionResponse<ExternalVocabularyResponse>> searchExternalVocabularies(
            HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        List<String> uris = externalSearchUris(body, issues);
        ApiSupport.failOnIssues(issues);
        List<ExternalVocabulary> found = externals.search(uris);
        return ApiSupport.json(Representations.unpaged(
                found.stream().map(Representations::external).toList()));
    }

    /** The oneOf of ExternalVocabularySearchRequest with the limits of D38 and the issue names of D68. */
    static List<String> externalSearchUris(JsonNode body, List<ProblemIssue> issues) {
        JsonBody.unknownProperties(body, EXTERNAL_SEARCH_FIELDS, "", issues);
        if (!issues.isEmpty()) {
            return List.of();
        }
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(ProblemIssue.schemaViolation(
                    "body",
                    "body",
                    "Give exactly one of namespaceUri, aliasUri, or namespaceUris and aliasUris.",
                    null));
            return List.of();
        }
        if (single || alias) {
            String field = single ? "namespaceUri" : "aliasUri";
            String uri = uri(body.get(field), field, issues);
            return uri == null ? List.of() : List.of(uri);
        }
        List<String> uris = new ArrayList<>();
        uris.addAll(uriArray(body, "namespaceUris", issues));
        uris.addAll(uriArray(body, "aliasUris", issues));
        if (uris.size() > MAX_REFERENCES) {
            String field = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
            issues.add(ProblemIssue.schemaViolation(
                    "body", field, "At most 100 URIs may be searched in one request.", null));
        }
        return uris;
    }

    private static List<String> uriArray(JsonNode body, String field, List<ProblemIssue> issues) {
        if (!body.has(field)) {
            return List.of();
        }
        JsonNode array = body.get(field);
        if (!array.isArray() || array.isEmpty()) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", field, "'" + field + "' must be an array with at least one URI.", null));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            String uri = uri(array.get(index), field + "[" + index + "]", issues);
            if (uri != null) {
                uris.add(uri);
            }
        }
        return uris;
    }

    private static String uri(JsonNode node, String field, List<ProblemIssue> issues) {
        if (node == null || !node.isString() || !RequestRules.absoluteUri(node.asString())) {
            issues.add(ProblemIssue.schemaViolation("body", field, "'" + field + "' must be an absolute URI.", null));
            return null;
        }
        return node.asString();
    }

    /** Used by POST /external-vocabularies/content. */
    static String contentRequestUri(JsonNode body, List<ProblemIssue> issues) {
        JsonBody.unknownProperties(body, Set.of("namespaceUri"), "", issues);
        if (!body.has("namespaceUri")) {
            issues.add(ProblemIssue.schemaViolation("body", "namespaceUri", "'namespaceUri' is required.", null));
            return null;
        }
        return uri(body.get("namespaceUri"), "namespaceUri", issues);
    }
}

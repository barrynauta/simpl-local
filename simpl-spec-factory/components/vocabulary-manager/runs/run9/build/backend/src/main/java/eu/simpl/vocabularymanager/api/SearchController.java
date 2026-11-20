package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.SearchReference;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.validation.UriKeys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** The read-only search operations (D38, D66, D68). */
@RestController
public class SearchController {

    /** Maximum references or URIs of one search (D38). */
    public static final int MAX_REFERENCES = 100;

    private static final Set<String> SEARCH_PROPERTIES = Set.of("items");
    private static final Set<String> ITEM_PROPERTIES = Set.of("name", "version");
    private static final Set<String> EXTERNAL_PROPERTIES =
            Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");

    private final VocabularyService vocabularies;
    private final ExternalVocabularyService externals;
    private final Representations representations;
    private final int maxJsonBytes;

    /**
     * @param vocabularies vocabulary service
     * @param externals external vocabulary service
     * @param representations wire representations
     * @param properties component configuration
     */
    public SearchController(
            VocabularyService vocabularies,
            ExternalVocabularyService externals,
            Representations representations,
            eu.simpl.vocabularymanager.config.VocabularyManagerProperties properties) {
        this.vocabularies = vocabularies;
        this.externals = externals;
        this.representations = representations;
        this.maxJsonBytes = properties.limits().maxJsonBytes();
    }

    /** POST /vocabularies/searches. */
    @PostMapping("/v1/vocabularies/searches")
    public ResponseEntity<byte[]> searchVocabularies(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeJson(request);
        JsonNode body = JsonBody.readObject(request, maxJsonBytes);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        unknownProperties(body, SEARCH_PROPERTIES, "", issues);
        List<SearchReference> references = references(body.get("items"), issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        List<Map<String, Object>> items = vocabularies.search(references).stream()
                .map(representations::searchItem)
                .toList();
        return JsonResponses.json(200, Representations.searchPage(items));
    }

    private static List<SearchReference> references(JsonNode items, List<Issue> issues) {
        List<SearchReference> references = new ArrayList<>();
        if (items == null) {
            issues.add(Issue.schemaViolation("body", "items", "Property 'items' is required.", null));
            return references;
        }
        if (!items.isArray() || items.size() < 1 || items.size() > MAX_REFERENCES) {
            issues.add(Issue.schemaViolation("body", "items", "Property 'items' must hold 1 to 100 references.", null));
            return references;
        }
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String prefix = "items[" + i + "]";
            if (!item.isObject()) {
                issues.add(Issue.schemaViolation("body", prefix, "Each item must be an object.", null));
                continue;
            }
            unknownProperties(item, ITEM_PROPERTIES, prefix + ".", issues);
            JsonNode name = item.get("name");
            String nameValue = name != null && name.isString() ? name.asString() : null;
            Issue nameIssue = RequestChecks.nameIssue(nameValue, "body", prefix + ".name");
            if (nameIssue != null) {
                issues.add(nameIssue);
            }
            Integer version = null;
            JsonNode versionNode = item.get("version");
            if (versionNode != null) {
                version = JsonBody.positiveInt(versionNode);
                if (version == null) {
                    issues.add(Issue.schemaViolation(
                            "body",
                            prefix + ".version",
                            "Property 'version' must be an integer from 1 to 2147483647.",
                            null));
                }
            }
            if (nameIssue == null) {
                references.add(new SearchReference(nameValue, version));
            }
        }
        return references;
    }

    /** POST /external-vocabulary/searches. */
    @PostMapping("/v1/external-vocabulary/searches")
    public ResponseEntity<byte[]> searchExternal(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeJson(request);
        JsonNode body = JsonBody.readObject(request, maxJsonBytes);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        unknownProperties(body, EXTERNAL_PROPERTIES, "", issues);
        List<String> uris = externalUris(body, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        List<ExternalVocabulary> found = externals.search(uris);
        return JsonResponses.json(
                200,
                Representations.searchPage(
                        found.stream().map(representations::external).toList()));
    }

    /** The URIs of an external search: one oneOf branch exactly (D38, D68, D73). */
    static List<String> externalUris(JsonNode body, List<Issue> issues) {
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        List<String> uris = new ArrayList<>();
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "body",
                    "Send exactly one of namespaceUri, aliasUri, or namespaceUris and aliasUris.",
                    null));
            return uris;
        }
        if (single) {
            uri(body.get("namespaceUri"), "namespaceUri", uris, issues);
            return uris;
        }
        if (alias) {
            uri(body.get("aliasUri"), "aliasUri", uris, issues);
            return uris;
        }
        int total = 0;
        for (String field : List.of("namespaceUris", "aliasUris")) {
            JsonNode array = body.get(field);
            if (array == null) {
                continue;
            }
            if (!array.isArray() || array.size() < 1) {
                issues.add(Issue.schemaViolation(
                        "body", field, "Property '" + field + "' must be an array with at least 1 URI.", null));
                continue;
            }
            total += array.size();
            for (int i = 0; i < array.size(); i++) {
                uri(array.get(i), field, uris, issues);
            }
        }
        if (total > MAX_REFERENCES) {
            String field = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
            issues.add(Issue.schemaViolation("body", field, "A search may hold at most 100 URIs in total.", null));
        }
        return uris;
    }

    private static void uri(JsonNode node, String field, List<String> uris, List<Issue> issues) {
        if (node == null || !node.isString() || !UriKeys.isAbsolute(node.asString())) {
            issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' must be an absolute URI.", null));
            return;
        }
        uris.add(node.asString());
    }

    /** One issue per property the schema does not define (D52). */
    static void unknownProperties(JsonNode node, Set<String> allowed, String prefix, List<Issue> issues) {
        for (String property : node.propertyNames()) {
            if (!allowed.contains(property)) {
                issues.add(Issue.schemaViolation(
                        "body", prefix + property, "Property '" + property + "' is not allowed.", null));
            }
        }
    }
}

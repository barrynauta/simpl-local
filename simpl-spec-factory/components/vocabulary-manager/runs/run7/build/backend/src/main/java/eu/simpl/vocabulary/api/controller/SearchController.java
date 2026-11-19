package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.ApiMapper;
import eu.simpl.vocabulary.api.dto.ExternalVocabularyDto;
import eu.simpl.vocabulary.api.dto.PageDto;
import eu.simpl.vocabulary.api.dto.SearchItemDto;
import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.api.request.QueryParameters;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.service.ExternalVocabularyService;
import eu.simpl.vocabulary.service.SearchService;
import eu.simpl.vocabulary.service.SearchService.SearchReference;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** The read-only POST operations: searches (D38, D68) and content by namespace (D66). */
@RestController
@RequestMapping("/v1")
public class SearchController {

    static final int MAX_ITEMS = 100;

    private static final Set<String> EXTERNAL_SEARCH_PROPERTIES =
            Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");

    private final SearchService searchService;
    private final ExternalVocabularyService externalService;
    private final ApiMapper mapper;
    private final AppProperties.Limits limits;

    public SearchController(
            SearchService searchService,
            ExternalVocabularyService externalService,
            ApiMapper mapper,
            AppProperties properties) {
        this.searchService = searchService;
        this.externalService = externalService;
        this.mapper = mapper;
        this.limits = properties.limits();
    }

    @PostMapping("/vocabularies/searches")
    public ResponseEntity<PageDto<SearchItemDto>> searchVocabularies(HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        JsonNode body = Requests.readJson(request, limits.maxJsonBytes(), issues);
        List<SearchReference> references = body == null ? List.of() : internalReferences(body, issues);
        Requests.rejectIfAny(issues);
        List<SearchItemDto> items = searchService.searchInternal(references).stream()
                .map(mapper::searchItem)
                .toList();
        return Requests.json(200, ApiMapper.unpaged(items));
    }

    /**
     * Validates the body of an internal search (VocabularySearchRequest, D38, D52, D68).
     *
     * @param body the body
     * @param issues issue sink
     * @return the references
     */
    static List<SearchReference> internalReferences(JsonNode body, List<Issue> issues) {
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "body", "The body must be a JSON object."));
            return List.of();
        }
        unknownProperties(body, Set.of("items"), "", issues);
        JsonNode items = body.get("items");
        if (items == null) {
            issues.add(FieldRules.required("items"));
            return List.of();
        }
        if (!items.isArray() || items.size() < 1 || items.size() > MAX_ITEMS) {
            issues.add(
                    Issue.schemaViolation(Issue.IN_BODY, "items", "'items' must be an array of 1 to 100 references."));
            return List.of();
        }
        List<SearchReference> references = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String prefix = "items[" + i + "]";
            if (!item.isObject()) {
                issues.add(Issue.schemaViolation(Issue.IN_BODY, prefix, "Each item must be a JSON object."));
                continue;
            }
            unknownProperties(item, Set.of("name", "version"), prefix + ".", issues);
            JsonNode name = item.get("name");
            boolean valid = true;
            if (name == null || !name.isString() || !FieldRules.isValidName(name.asString())) {
                issues.add(
                        Issue.schemaViolation(
                                Issue.IN_BODY,
                                prefix + ".name",
                                "'name' is required and must be 3 to 64 letters or digits starting with an upper-case letter."));
                valid = false;
            }
            Long version = null;
            JsonNode versionNode = item.get("version");
            if (versionNode != null) {
                version = positiveInteger(versionNode);
                if (version == null) {
                    issues.add(Issue.schemaViolation(
                            Issue.IN_BODY, prefix + ".version", "'version' must be an integer of at least 1."));
                    valid = false;
                }
            }
            if (valid) {
                references.add(new SearchReference(name.asString(), version));
            }
        }
        return references;
    }

    private static Long positiveInteger(JsonNode node) {
        if (!node.isNumber()) {
            return null;
        }
        java.math.BigDecimal value = node.decimalValue();
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 0) {
            return null;
        }
        java.math.BigInteger integer = value.toBigIntegerExact();
        return integer.bitLength() > 62 ? Long.MAX_VALUE : integer.longValue();
    }

    @PostMapping("/external-vocabulary/searches")
    public ResponseEntity<PageDto<ExternalVocabularyDto>> searchExternal(HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        JsonNode body = Requests.readJson(request, limits.maxJsonBytes(), issues);
        List<String> uris = body == null ? List.of() : externalUris(body, issues);
        Requests.rejectIfAny(issues);
        List<ExternalVocabularyDto> items = searchService.searchExternal(uris).stream()
                .map(mapper::external)
                .toList();
        return Requests.json(200, ApiMapper.unpaged(items));
    }

    /**
     * Validates the body of an external search (ExternalVocabularySearchRequest oneOf, D38, D68, D73).
     *
     * @param body the body
     * @param issues issue sink
     * @return the URIs, namespaceUris before aliasUris
     */
    static List<String> externalUris(JsonNode body, List<Issue> issues) {
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "body", "The body must be a JSON object."));
            return List.of();
        }
        int before = issues.size();
        unknownProperties(body, EXTERNAL_SEARCH_PROPERTIES, "", issues);
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY,
                    "body",
                    "Send exactly one of: namespaceUri, aliasUri, or namespaceUris and/or aliasUris."));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        if (single) {
            uri(body, "namespaceUri", issues).ifPresent(uris::add);
        } else if (alias) {
            uri(body, "aliasUri", issues).ifPresent(uris::add);
        } else {
            int total = 0;
            for (String field : List.of("namespaceUris", "aliasUris")) {
                JsonNode array = body.get(field);
                if (array == null) {
                    continue;
                }
                if (!array.isArray() || array.size() < 1) {
                    issues.add(Issue.schemaViolation(
                            Issue.IN_BODY, field, "'" + field + "' must be an array of at least 1 URI."));
                    continue;
                }
                total += array.size();
                for (JsonNode element : array) {
                    if (element.isString() && FieldRules.isAbsoluteUri(element.asString())) {
                        uris.add(element.asString());
                    } else {
                        issues.add(Issue.schemaViolation(
                                Issue.IN_BODY, field, "'" + field + "' must hold absolute URIs."));
                        break;
                    }
                }
            }
            if (total > MAX_ITEMS) {
                String name = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "At most 100 URIs may be searched at once."));
            }
        }
        return issues.size() > before ? List.of() : uris;
    }

    @PostMapping(value = "/external-vocabularies/content", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> contentByNamespace(HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        JsonNode body = Requests.readJson(request, limits.maxJsonBytes(), issues);
        String namespaceUri = null;
        if (body != null) {
            if (!body.isObject()) {
                issues.add(Issue.schemaViolation(Issue.IN_BODY, "body", "The body must be a JSON object."));
            } else {
                unknownProperties(body, Set.of("namespaceUri"), "", issues);
                if (!body.has("namespaceUri")) {
                    issues.add(FieldRules.required("namespaceUri"));
                } else {
                    namespaceUri = uri(body, "namespaceUri", issues).orElse(null);
                }
            }
        }
        Requests.rejectIfAny(issues);
        StoredContent content = externalService.contentByNamespace(namespaceUri, searchService);
        return Requests.turtle(content.bytes(), content.name() + ".ttl");
    }

    private static java.util.Optional<String> uri(JsonNode body, String field, List<Issue> issues) {
        JsonNode value = body.get(field);
        if (value != null && value.isString() && FieldRules.isAbsoluteUri(value.asString())) {
            return java.util.Optional.of(value.asString());
        }
        issues.add(Issue.schemaViolation(Issue.IN_BODY, field, "'" + field + "' must be an absolute URI."));
        return java.util.Optional.empty();
    }

    private static void unknownProperties(JsonNode object, Set<String> known, String prefix, List<Issue> issues) {
        for (Map.Entry<String, JsonNode> property : Requests.properties(object)) {
            if (!known.contains(property.getKey())) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY,
                        prefix + property.getKey(),
                        "Property '" + property.getKey() + "' is not supported."));
            }
        }
    }
}

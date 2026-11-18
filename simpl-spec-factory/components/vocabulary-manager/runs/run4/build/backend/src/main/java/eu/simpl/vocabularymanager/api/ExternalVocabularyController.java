package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.vocabularymanager.api.ApiModel.CollectionPage;
import eu.simpl.vocabularymanager.api.ApiModel.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Namespaces;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.service.Caller;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService.NamedContent;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService.Register;
import eu.simpl.vocabularymanager.service.PageRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** External vocabulary management operations of the OpenAPI (decisions D30, D38, D41, D42). */
@RestController
public class ExternalVocabularyController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String MULTIPART = MediaType.MULTIPART_FORM_DATA_VALUE;
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalVocabularyService service;
    private final ApiMapper mapper;
    private final JsonBodies jsonBodies;
    private final long maxUploadBytes;

    public ExternalVocabularyController(
            ExternalVocabularyService service,
            ApiMapper mapper,
            JsonBodies jsonBodies,
            VocabularyManagerProperties properties) {
        this.service = service;
        this.mapper = mapper;
        this.jsonBodies = jsonBodies;
        this.maxUploadBytes = properties.maxUploadBytes();
    }

    /** GET /external-vocabularies: paged, name ascending, no filters. */
    @GetMapping(value = "/external-vocabularies", produces = JSON)
    public CollectionPage<ExternalVocabularyResponse> list(HttpServletRequest request) {
        QueryParameters query = QueryParameters.read(request, Set.of("page", "pageSize"));
        List<Issue> issues = new ArrayList<>();
        PageRequest pageRequest = query.pageRequest(issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return mapper.page(service.list(pageRequest), "/external-vocabularies", Map.of(), mapper::external);
    }

    /** POST /external-vocabularies: register after a syntax check. */
    @PostMapping(value = "/external-vocabularies", consumes = MULTIPART, produces = JSON)
    public ResponseEntity<ExternalVocabularyResponse> register(
            HttpServletRequest request, Authentication authentication) throws IOException, ServletException {
        QueryParameters.read(request, Set.of());
        MultipartForm form = MultipartForm.read(
                request,
                VocabularyController.FILE,
                Set.of(VocabularyController.FILE, "name", "namespaceUri", "aliasUris", "description", "sourceUri"),
                Map.of());
        List<Issue> issues = new ArrayList<>();
        String name = FormFields.name(form, issues);
        String namespaceUri = requiredUri(form, issues);
        List<String> aliasUris = aliasUris(form, namespaceUri, issues);
        String sourceUri = optionalUri(form, "sourceUri", issues);
        String description = FormFields.optionalText(form, "description", issues);
        byte[] content = FormFields.file(form, VocabularyController.FILE, issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        if (content.length > maxUploadBytes) {
            throw Problems.uploadTooLarge();
        }
        ExternalVocabulary registered = service.register(new Register(
                name, namespaceUri, aliasUris, sourceUri, description, content, new Caller(authentication.getName())));
        ExternalVocabularyResponse body = mapper.external(registered);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** POST /external-vocabulary/searches (decision D38). */
    @PostMapping(value = "/external-vocabulary/searches", consumes = JSON, produces = JSON)
    public CollectionPage<ExternalVocabularyResponse> search(
            HttpServletRequest request, @RequestBody(required = false) String body) {
        QueryParameters.read(request, Set.of());
        JsonNode node = jsonBodies.object(body, Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris"));
        boolean single = node.has("namespaceUri");
        boolean alias = node.has("aliasUri");
        boolean batch = node.has("namespaceUris") || node.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            throw Problems.badRequest(Issue.schemaViolation(
                    "body",
                    null,
                    null,
                    "Give exactly one of: namespaceUri, aliasUri, or namespaceUris and/or aliasUris."));
        }
        List<Issue> issues = new ArrayList<>();
        List<String> uris = new ArrayList<>();
        if (single) {
            uris.add(uriProperty(node, "namespaceUri", issues));
        } else if (alias) {
            uris.add(uriProperty(node, "aliasUri", issues));
        } else {
            uris.addAll(uriArray(node, "namespaceUris", issues));
            uris.addAll(uriArray(node, "aliasUris", issues));
            if (uris.size() > MAX_SEARCH_URIS) {
                String field = node.has("aliasUris") && !node.has("namespaceUris") ? "aliasUris" : "namespaceUris";
                issues.add(Issue.schemaViolation(
                        "body", field, null, "At most " + MAX_SEARCH_URIS + " URIs may be searched in one request."));
            }
        }
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return mapper.searchResult(
                service.search(uris).stream().map(mapper::external).toList());
    }

    /** POST /external-vocabularies/content: content by namespace URI or alias. */
    @PostMapping(value = "/external-vocabularies/content", consumes = JSON, produces = ContentController.TURTLE)
    public ResponseEntity<byte[]> contentByNamespace(
            HttpServletRequest request, @RequestBody(required = false) String body) {
        QueryParameters.read(request, Set.of());
        JsonNode node = jsonBodies.object(body, Set.of("namespaceUri"));
        List<Issue> issues = new ArrayList<>();
        String namespaceUri = uriProperty(node, "namespaceUri", issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        NamedContent content = service.contentByNamespace(namespaceUri);
        return ContentController.turtle(content.bytes(), content.name() + ".ttl");
    }

    /** GET /external-vocabularies/{name}/content. */
    @GetMapping(value = "/external-vocabularies/{name}/content", produces = ContentController.TURTLE)
    public ResponseEntity<byte[]> content(@PathVariable("name") String name, HttpServletRequest request) {
        String checked = PathParameters.name("name", name);
        QueryParameters.read(request, Set.of());
        return ContentController.turtle(service.content(checked), checked + ".ttl");
    }

    /** GET /external-vocabularies/{name}. */
    @GetMapping(value = "/external-vocabularies/{name}", produces = JSON)
    public ExternalVocabularyResponse get(@PathVariable("name") String name, HttpServletRequest request) {
        String checked = PathParameters.name("name", name);
        QueryParameters.read(request, Set.of());
        return mapper.external(service.get(checked));
    }

    /** DELETE /external-vocabularies/{name}: 204, no reference check (decision D42). */
    @DeleteMapping("/external-vocabularies/{name}")
    public ResponseEntity<Void> delete(@PathVariable("name") String name, HttpServletRequest request) {
        String checked = PathParameters.name("name", name);
        QueryParameters.read(request, Set.of());
        service.delete(checked);
        return ResponseEntity.noContent().build();
    }

    private static String requiredUri(MultipartForm form, List<Issue> issues) {
        if (form.count("namespaceUri") != 1) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", null, "Field 'namespaceUri' is required and must be given once."));
            return null;
        }
        String value = form.value("namespaceUri").trim();
        if (!FormFields.isAbsoluteUri(value)) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", value, "Field 'namespaceUri' must be an absolute URI."));
            return null;
        }
        return value;
    }

    private static String optionalUri(MultipartForm form, String field, List<Issue> issues) {
        if (form.count(field) == 0 || form.value(field).isBlank()) {
            return null;
        }
        String value = form.value(field).trim();
        if (form.count(field) > 1 || !FormFields.isAbsoluteUri(value)) {
            issues.add(Issue.schemaViolation("body", field, value, "Field '" + field + "' must be one absolute URI."));
            return null;
        }
        return value;
    }

    /** Alias URIs as repeated fields or as one JSON array; duplicates and the namespace URI itself are dropped. */
    private List<String> aliasUris(MultipartForm form, String namespaceUri, List<Issue> issues) {
        List<String> raw = new ArrayList<>();
        for (String value : form.values("aliasUris")) {
            String trimmed = value.trim();
            if (trimmed.startsWith("[")) {
                JsonNode array = jsonArray(trimmed);
                if (array == null) {
                    issues.add(Issue.schemaViolation(
                            "body", "aliasUris", trimmed, "Field 'aliasUris' must hold absolute URIs."));
                    return List.of();
                }
                array.forEach(item -> raw.add(item.isTextual() ? item.asText().trim() : item.toString()));
            } else if (!trimmed.isEmpty()) {
                raw.add(trimmed);
            }
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String uri : raw) {
            if (!FormFields.isAbsoluteUri(uri)) {
                issues.add(
                        Issue.schemaViolation("body", "aliasUris", uri, "Field 'aliasUris' must hold absolute URIs."));
                continue;
            }
            String key = Namespaces.normalise(uri);
            if (namespaceUri == null || !key.equals(Namespaces.normalise(namespaceUri))) {
                unique.putIfAbsent(key, uri);
            }
        }
        return List.copyOf(unique.values());
    }

    private JsonNode jsonArray(String text) {
        try {
            JsonNode node = jsonBodies.parse(text);
            return node != null && node.isArray() ? node : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String uriProperty(JsonNode node, String field, List<Issue> issues) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || !FormFields.isAbsoluteUri(value.asText())) {
            issues.add(Issue.schemaViolation(
                    "body",
                    field,
                    value == null ? null : value.asText(),
                    "Property '" + field + "' is required and must be an absolute URI."));
            return null;
        }
        return value.asText();
    }

    private static List<String> uriArray(JsonNode node, String field, List<Issue> issues) {
        JsonNode array = node.get(field);
        if (array == null) {
            return List.of();
        }
        if (!array.isArray() || array.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    "body", field, null, "Property '" + field + "' must be an array with at least 1 URI."));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isTextual() || !FormFields.isAbsoluteUri(item.asText())) {
                issues.add(Issue.schemaViolation(
                        "body", field, item.asText(), "Property '" + field + "' must hold absolute URIs."));
            } else {
                uris.add(item.asText());
            }
        }
        return uris;
    }
}

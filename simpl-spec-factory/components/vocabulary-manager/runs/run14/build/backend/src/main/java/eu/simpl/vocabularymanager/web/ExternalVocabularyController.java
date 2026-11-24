package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.api.PageResponse;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Operations on external vocabularies (OpenAPI tag "External vocabulary management", D41). */
@RestController
@RequestMapping("/v1")
public class ExternalVocabularyController {

    private static final String JSON = VocabularyController.JSON;
    private static final String PROBLEM = VocabularyController.PROBLEM;
    private static final Set<String> FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "description", "sourceUri");
    private static final int MAX_ALIASES = 20;
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalVocabularyService service;
    private final ApiMapper mapper;

    public ExternalVocabularyController(ExternalVocabularyService service, ApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** GET /external-vocabularies, paged, name order only (D41). */
    @GetMapping(
            path = "/external-vocabularies",
            produces = {JSON, PROBLEM})
    public ResponseEntity<PageResponse<ExternalVocabularyResponse>> getExternalVocabularies(
            HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        query.requireOnly(Set.of("page", "pageSize"));
        int page = RequestRules.queryInteger("page", query.get("page"), 1, RequestRules.MAX_INT);
        int pageSize = RequestRules.queryInteger(
                "pageSize", query.get("pageSize"), Paging.DEFAULT_PAGE_SIZE, Paging.MAX_PAGE_SIZE);
        VocabularyService.Slice<ExternalVocabulary> slice = service.list((long) (page - 1) * pageSize, pageSize);
        List<ExternalVocabularyResponse> items =
                slice.items().stream().map(mapper::external).toList();
        return VocabularyController.json(
                Paging.page("/external-vocabularies", query, page, pageSize, slice.total(), items));
    }

    /** POST /external-vocabularies: register (D30, D56, D70, D73, D147, D148). */
    @PostMapping(
            path = "/external-vocabularies",
            consumes = "multipart/form-data",
            produces = {JSON, PROBLEM})
    public ResponseEntity<ExternalVocabularyResponse> createExternalVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        form.checkShape(FIELDS, Set.of("aliasUris"));
        List<Issue> issues = new ArrayList<>();
        String name = RequestRules.nameField(form.text("name"), issues);
        String namespace = form.text("namespaceUri");
        if (namespace == null) {
            issues.add(Issue.schemaViolation("body", "namespaceUri", "Field 'namespaceUri' is required."));
        } else if (!RequestRules.isAbsoluteUri(namespace)) {
            issues.add(Issue.schemaViolation("body", "namespaceUri", "Field 'namespaceUri' must be an absolute URI."));
        }
        List<String> aliases = form.texts("aliasUris");
        checkAliases(namespace, aliases, issues);
        String description = RequestRules.optionalText("description", form.text("description"), issues);
        String source = form.text("sourceUri");
        if (source != null && !RequestRules.isAbsoluteUri(source)) {
            issues.add(Issue.schemaViolation("body", "sourceUri", "Field 'sourceUri' must be an absolute URI."));
        }
        byte[] file = form.bytes(MultipartForm.FILE_FIELD);
        if (file == null) {
            issues.add(Issue.schemaViolation("body", MultipartForm.FILE_FIELD, "Field 'vocabularyFile' is required."));
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        ExternalVocabulary registered = service.register(
                new ExternalVocabularyService.Registration(name, namespace, aliases, source, description, file),
                VocabularyController.user(request));
        ExternalVocabularyResponse body = mapper.external(registered);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** POST /external-vocabulary/searches (D38, D68, D137, D182). */
    @PostMapping(
            path = "/external-vocabulary/searches",
            consumes = JSON,
            produces = {JSON, PROBLEM})
    public ResponseEntity<PageResponse<ExternalVocabularyResponse>> searchExternalVocabularies(
            HttpServletRequest request) {
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        JsonNode body = JsonBody.readObject(request);
        JsonBody.requireOnly(body, "", Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris"));
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            throw ApiException.badRequest(
                    "body", "body", "Send exactly one of namespaceUri, aliasUri, or namespaceUris and/or aliasUris.");
        }
        List<String> uris = new ArrayList<>();
        if (single) {
            uris.add(uri(body.get("namespaceUri"), "namespaceUri"));
        } else if (alias) {
            uris.add(uri(body.get("aliasUri"), "aliasUri"));
        } else {
            List<String> namespaces = uriArray(body, "namespaceUris");
            List<String> aliases = uriArray(body, "aliasUris");
            if (namespaces.size() + aliases.size() > MAX_SEARCH_URIS) {
                String array = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                throw ApiException.badRequest("body", array, "A search takes at most 100 URIs in total.");
            }
            uris.addAll(namespaces);
            uris.addAll(aliases);
        }
        List<ExternalVocabularyResponse> results =
                service.search(uris).stream().map(mapper::external).toList();
        return VocabularyController.json(Paging.searchPage(results));
    }

    /** POST /external-vocabularies/content: content by namespace or alias URI (D66). */
    @PostMapping(
            path = "/external-vocabularies/content",
            consumes = JSON,
            produces = {ContentController.TURTLE_TYPE, ContentController.ANY_APPLICATION})
    public ResponseEntity<byte[]> downloadExternalVocabularyContent(HttpServletRequest request) {
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        JsonNode body = JsonBody.readObject(request);
        JsonBody.requireOnly(body, "", Set.of("namespaceUri"));
        String namespace = uri(body.get("namespaceUri"), "namespaceUri");
        Map.Entry<ExternalVocabulary, byte[]> found = service.contentByNamespace(namespace);
        return ContentController.turtle(found.getValue(), found.getKey().name() + ".ttl");
    }

    /** GET /external-vocabularies/{name}/content (D41). */
    @GetMapping(
            path = "/external-vocabularies/{name}/content",
            produces = {ContentController.TURTLE_TYPE, ContentController.ANY_APPLICATION})
    public ResponseEntity<byte[]> getExternalVocabularyContentByName(
            @PathVariable("name") String rawName, HttpServletRequest request) {
        String name = RequestRules.externalName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        return ContentController.turtle(service.content(name), name + ".ttl");
    }

    /** GET /external-vocabularies/{name} (D41). */
    @GetMapping(
            path = "/external-vocabularies/{name}",
            produces = {JSON, PROBLEM})
    public ResponseEntity<ExternalVocabularyResponse> getExternalVocabulary(
            @PathVariable("name") String rawName, HttpServletRequest request) {
        String name = RequestRules.externalName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        return VocabularyController.json(mapper.external(service.get(name)));
    }

    /** DELETE /external-vocabularies/{name} (D41, D42). */
    @DeleteMapping(path = "/external-vocabularies/{name}")
    public ResponseEntity<Void> deleteExternalVocabulary(
            @PathVariable("name") String rawName, HttpServletRequest request) {
        String name = RequestRules.externalName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        service.delete(name, VocabularyController.user(request));
        return ResponseEntity.noContent().build();
    }

    private static void checkAliases(String namespace, List<String> aliases, List<Issue> issues) {
        if (aliases.size() > MAX_ALIASES) {
            issues.add(Issue.schemaViolation("body", "aliasUris", "At most 20 aliasUris are allowed."));
            return;
        }
        Set<String> seen = new HashSet<>();
        if (namespace != null) {
            seen.add(eu.simpl.vocabularymanager.validation.References.normalise(namespace));
        }
        for (String alias : aliases) {
            if (!RequestRules.isAbsoluteUri(alias)) {
                issues.add(
                        Issue.schemaViolation("body", "aliasUris", "Every aliasUris value must be an absolute URI."));
                return;
            }
            if (!seen.add(eu.simpl.vocabularymanager.validation.References.normalise(alias))) {
                issues.add(Issue.schemaViolation(
                        "body", "aliasUris", "An alias URI repeats the namespace URI or another alias."));
                return;
            }
        }
    }

    private static String uri(JsonNode node, String name) {
        if (node == null || !node.isString() || !RequestRules.isAbsoluteUri(node.asString())) {
            throw ApiException.badRequest("body", name, "Property '" + name + "' must be an absolute URI.");
        }
        return node.asString();
    }

    private static List<String> uriArray(JsonNode body, String name) {
        JsonNode array = body.get(name);
        if (array == null) {
            return List.of();
        }
        if (!array.isArray() || array.isEmpty()) {
            throw ApiException.badRequest("body", name, "Property '" + name + "' must be a non-empty array.");
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(uri(array.get(i), name + "[" + i + "]"));
        }
        return values;
    }
}

package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.Dtos;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.request.FieldRules;
import eu.simpl.vocabularymanager.request.JsonBody;
import eu.simpl.vocabularymanager.request.MultipartForm;
import eu.simpl.vocabularymanager.request.Paging;
import eu.simpl.vocabularymanager.request.QueryParameters;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.SearchService;
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
import org.springframework.web.bind.annotation.RestController;

/** External vocabulary operations (tag "External vocabulary management", D30, D38, D41, D56, D66, D73). */
@RestController
public class ExternalVocabularyController {

    static final Set<String> FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    static final Set<String> SEARCH_FIELDS = Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");
    static final int SEARCH_MAX = 100;

    private final ExternalVocabularyService externals;
    private final SearchService search;
    private final Representations representations;

    public ExternalVocabularyController(
            ExternalVocabularyService externals, SearchService search, Representations representations) {
        this.externals = externals;
        this.search = search;
        this.representations = representations;
    }

    @GetMapping("/v1/external-vocabularies")
    public ResponseEntity<Dtos.CollectionPage<Dtos.ExternalVocabulary>> getExternalVocabularies(
            HttpServletRequest request) {
        Checks checks = new Checks();
        QueryParameters parameters = checks.query(request, Set.of("page", "pageSize"));
        Paging paging = Paging.from(parameters, checks.issues);
        checks.throwIfAny();
        VocabularyService.Page<ExternalVocabulary> page = externals.list(paging.offset(), paging.pageSize());
        List<Dtos.ExternalVocabulary> items =
                page.items().stream().map(representations::external).toList();
        return VocabularyController.json(
                Representations.page(items, page.total(), paging, "/external-vocabularies", parameters));
    }

    @PostMapping("/v1/external-vocabularies")
    public ResponseEntity<Dtos.ExternalVocabulary> createExternalVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request, "vocabularyFile");
        Checks checks = new Checks();
        checks.noQuery(request);
        form.checkNames(FIELDS, Set.of("aliasUris"), checks.issues);
        String name = FieldRules.name(form.text("name"), "body", "name", checks.issues);
        String namespaceUri = FieldRules.uri(form.text("namespaceUri"), "body", "namespaceUri", true, checks.issues);
        List<String> aliasUris = aliases(form.texts("aliasUris"), namespaceUri, checks.issues);
        String sourceUri = FieldRules.uri(form.text("sourceUri"), "body", "sourceUri", false, checks.issues);
        String description = FieldRules.text(form.text("description"), "description", false, checks.issues);
        byte[] content = form.bytes("vocabularyFile");
        if (content == null) {
            checks.issues.add(Issue.schemaViolation("body", "vocabularyFile", "'vocabularyFile' is required.", null));
        }
        checks.throwIfAny();
        ExternalVocabulary registered = externals.register(new ExternalVocabularyService.Registration(
                name,
                namespaceUri,
                aliasUris,
                sourceUri,
                description,
                content,
                VocabularyController.currentUser().creator()));
        Dtos.ExternalVocabulary body = representations.external(registered);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping("/v1/external-vocabulary/searches")
    public ResponseEntity<Dtos.CollectionPage<Dtos.ExternalVocabulary>> searchExternalVocabularies(
            HttpServletRequest request) {
        Map<String, Object> body = JsonBody.read(request);
        Checks checks = new Checks();
        checks.noQuery(request);
        List<String> uris = searchUris(body, checks.issues);
        checks.throwIfAny();
        List<Dtos.ExternalVocabulary> items = search.searchExternal(uris).stream()
                .map(representations::external)
                .toList();
        return VocabularyController.json(Representations.searchPage(items));
    }

    @PostMapping("/v1/external-vocabularies/content")
    public ResponseEntity<byte[]> downloadExternalVocabularyContent(HttpServletRequest request) {
        Map<String, Object> body = JsonBody.read(request);
        Checks checks = new Checks();
        checks.noQuery(request);
        for (String key : body.keySet()) {
            if (!"namespaceUri".equals(key)) {
                checks.issues.add(Issue.schemaViolation("body", key, "Property '" + key + "' is not supported.", null));
            }
        }
        Object value = body.get("namespaceUri");
        String uri = null;
        if (value == null) {
            checks.issues.add(Issue.schemaViolation("body", "namespaceUri", "'namespaceUri' is required.", null));
        } else if (!(value instanceof String text) || !FieldRules.isAbsoluteUri(text)) {
            checks.issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", "'namespaceUri' must be an absolute URI.", String.valueOf(value)));
        } else {
            uri = text;
        }
        checks.throwIfAny();
        Map.Entry<String, byte[]> content = externals.contentByNamespace(uri);
        return ContentController.turtle(content.getKey() + ".ttl", content.getValue());
    }

    @GetMapping("/v1/external-vocabularies/{name}/content")
    public ResponseEntity<byte[]> getExternalVocabularyContentByName(
            @PathVariable String name, HttpServletRequest request) {
        String checked = checkedName(name, request);
        return ContentController.turtle(checked + ".ttl", externals.content(checked));
    }

    @GetMapping("/v1/external-vocabularies/{name}")
    public ResponseEntity<Dtos.ExternalVocabulary> getExternalVocabulary(
            @PathVariable String name, HttpServletRequest request) {
        String checked = checkedName(name, request);
        return VocabularyController.json(representations.external(externals.get(checked)));
    }

    @DeleteMapping("/v1/external-vocabularies/{name}")
    public ResponseEntity<Void> deleteExternalVocabulary(@PathVariable String name, HttpServletRequest request) {
        String checked = checkedName(name, request);
        externals.delete(checked);
        return ResponseEntity.noContent().build();
    }

    private static String checkedName(String name, HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        String checked = checks.pathName(name, "name");
        checks.throwIfAny();
        return checked;
    }

    /** aliasUris (D56, D70, D73): at most 20, each one absolute URI, none equal to the namespace or another alias. */
    static List<String> aliases(List<String> values, String namespaceUri, List<Issue> issues) {
        if (values.size() > FieldRules.ALIAS_MAX) {
            issues.add(Issue.schemaViolation(
                    "body", "aliasUris", "At most " + FieldRules.ALIAS_MAX + " aliasUris are allowed.", null));
            return List.of();
        }
        Set<String> keys = new HashSet<>();
        if (namespaceUri != null) {
            keys.add(ExternalVocabulary.key(namespaceUri));
        }
        List<String> aliases = new ArrayList<>();
        for (String value : values) {
            if (!FieldRules.isAbsoluteUri(value)) {
                issues.add(Issue.schemaViolation("body", "aliasUris", "Each alias must be an absolute URI.", value));
                continue;
            }
            if (!keys.add(ExternalVocabulary.key(value))) {
                issues.add(Issue.schemaViolation(
                        "body", "aliasUris", "An alias may not repeat the namespace URI or another alias.", value));
                continue;
            }
            aliases.add(value);
        }
        return aliases;
    }

    /** The external search body (D38, D68, D110): exactly one oneOf branch; at most 100 URIs in total. */
    static List<String> searchUris(Map<String, Object> body, List<Issue> issues) {
        for (String key : body.keySet()) {
            if (!SEARCH_FIELDS.contains(key)) {
                issues.add(Issue.schemaViolation("body", key, "Property '" + key + "' is not supported.", null));
            }
        }
        boolean single = body.containsKey("namespaceUri");
        boolean alias = body.containsKey("aliasUri");
        boolean batch = body.containsKey("namespaceUris") || body.containsKey("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "body",
                    "Send exactly one of namespaceUri, aliasUri, or namespaceUris and/or aliasUris.",
                    null));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        if (single || alias) {
            String field = single ? "namespaceUri" : "aliasUri";
            Object value = body.get(field);
            if (value instanceof String text && FieldRules.isAbsoluteUri(text)) {
                uris.add(text);
            } else {
                issues.add(Issue.schemaViolation(
                        "body", field, "'" + field + "' must be an absolute URI.", String.valueOf(value)));
            }
            return uris;
        }
        uris.addAll(uriArray(body, "namespaceUris", issues));
        uris.addAll(uriArray(body, "aliasUris", issues));
        int total = sizeOf(body.get("namespaceUris")) + sizeOf(body.get("aliasUris"));
        if (total > SEARCH_MAX) {
            String name = body.containsKey("aliasUris") ? "aliasUris" : "namespaceUris";
            issues.add(Issue.schemaViolation("body", name, "At most " + SEARCH_MAX + " URIs in total.", null));
        }
        return uris;
    }

    private static int sizeOf(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private static List<String> uriArray(Map<String, Object> body, String field, List<Issue> issues) {
        if (!body.containsKey(field)) {
            return List.of();
        }
        if (!(body.get(field) instanceof List<?> list) || list.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    "body", field, "'" + field + "' must be an array with at least one URI.", null));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String text && FieldRules.isAbsoluteUri(text)) {
                uris.add(text);
            } else {
                issues.add(Issue.schemaViolation(
                        "body",
                        field + "[" + i + "]",
                        "Each URI must be an absolute URI.",
                        String.valueOf(list.get(i))));
            }
        }
        return uris;
    }
}

package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.ExternalService;
import eu.simpl.vocabularymanager.domain.Model;
import eu.simpl.vocabularymanager.domain.Model.Page;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.validation.References;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations (D30, D38, D41, D42, D56, D66, D68, D70, D73, D137, D147). */
@RestController
public class ExternalController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    public static final Set<String> FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final int MAX_ALIASES = 20;
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalService service;
    private final JsonBody json;

    public ExternalController(ExternalService service, JsonBody json) {
        this.service = service;
        this.json = json;
    }

    @GetMapping(path = "/v1/external-vocabularies", produces = JSON)
    public Dto.PageDto<Dto.ExternalDto> list(HttpServletRequest request) {
        QueryParams q = QueryParams.parse(request.getQueryString(), Set.of("page", "pageSize"));
        List<Issue> issues = RequestRules.issues();
        int page = q.positiveInt("page", 1, Integer.MAX_VALUE, issues);
        int pageSize = q.positiveInt("pageSize", 20, 100, issues);
        RequestRules.throwIfAny(issues);
        Page<Model.ExternalVocabulary> p = service.list(page, pageSize);
        return Dto.PageDto.paged(
                "/external-vocabularies",
                q.all(),
                page,
                pageSize,
                p.total(),
                p.items().stream().map(Dto.ExternalDto::of).toList());
    }

    @PostMapping(path = "/v1/external-vocabularies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = JSON)
    public ResponseEntity<Dto.ExternalDto> register(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request);
        RequestRules.noQuery(request.getQueryString());
        RequestRules.throwIfAny(form.shapeIssues(FIELDS, Set.of("aliasUris")));
        List<Issue> issues = RequestRules.issues();
        RequestRules.name(form.text("name"), form.has("name"), issues);
        String ns = form.text("namespaceUri");
        if (!form.has("namespaceUri")) {
            issues.add(Issue.schemaViolation("body", "namespaceUri", "Field 'namespaceUri' is required.", null));
        } else if (!Validation.absoluteUri(ns)) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", "Field 'namespaceUri' must be an absolute URI.", null));
        }
        List<String> aliases = form.texts("aliasUris");
        aliasIssues(aliases, ns, issues);
        String description = null;
        if (form.has("description")) {
            String d = form.text("description");
            if (form.oversized("description") || d.strip().length() > Validation.TEXT_MAX) {
                issues.add(Issue.schemaViolation(
                        "body",
                        "description",
                        "Field 'description' must be at most 1000 characters after trimming.",
                        null));
            } else if (!d.isBlank()) {
                description = d.strip();
            }
        }
        String source = null;
        if (form.has("sourceUri")) {
            source = form.text("sourceUri");
            if (!Validation.absoluteUri(source)) {
                issues.add(
                        Issue.schemaViolation("body", "sourceUri", "Field 'sourceUri' must be an absolute URI.", null));
            }
        }
        RequestRules.requiredFile(form, issues);
        RequestRules.throwIfAny(issues);
        Model.ExternalVocabulary e = service.register(
                new ExternalService.Registration(
                        form.text("name"), ns, List.copyOf(aliases), source, description, form.file()),
                RequestRules.user().creator());
        Dto.ExternalDto body = Dto.ExternalDto.of(e);
        return ResponseEntity.created(URI.create(body.href())).body(body);
    }

    private static void aliasIssues(List<String> aliases, String ns, List<Issue> issues) {
        if (aliases.size() > MAX_ALIASES) {
            issues.add(Issue.schemaViolation("body", "aliasUris", "At most 20 aliasUris are allowed.", null));
            return;
        }
        Set<String> keys = new HashSet<>();
        if (ns != null) {
            keys.add(References.normalise(ns));
        }
        for (String a : aliases) {
            if (!Validation.absoluteUri(a)) {
                issues.add(Issue.schemaViolation(
                        "body", "aliasUris", "Each aliasUris value must be an absolute URI.", null));
                return;
            }
            if (!keys.add(References.normalise(a))) {
                issues.add(Issue.schemaViolation(
                        "body", "aliasUris", "An alias URI may not repeat the namespace URI or another alias.", null));
                return;
            }
        }
    }

    @GetMapping(path = "/v1/external-vocabularies/{name}", produces = JSON)
    public Dto.ExternalDto get(@PathVariable("name") String name, HttpServletRequest request) {
        Validation.pathName("name", name);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return Dto.ExternalDto.of(service.get(name));
    }

    @DeleteMapping(path = "/v1/external-vocabularies/{name}", produces = JSON)
    public ResponseEntity<Void> delete(@PathVariable("name") String name, HttpServletRequest request) {
        Validation.pathName("name", name);
        RequestRules.noQuery(request.getQueryString());
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/v1/external-vocabularies/{name}/content", produces = "text/turtle")
    public ResponseEntity<byte[]> content(@PathVariable("name") String name, HttpServletRequest request) {
        Validation.pathName("name", name);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return VocabularyController.turtle(service.content(name), name + ".ttl");
    }

    @PostMapping(path = "/v1/external-vocabularies/content", consumes = JSON, produces = "text/turtle")
    public ResponseEntity<byte[]> contentByNamespace(HttpServletRequest request) {
        RequestRules.noQuery(request.getQueryString());
        JsonNode body = json.read(request);
        List<Issue> issues = RequestRules.issues();
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation("body", "body", "The body must be a JSON object.", null));
            RequestRules.throwIfAny(issues);
        }
        RequestRules.unknownProperties(body, Set.of("namespaceUri"), "", issues);
        JsonNode ns = body.get("namespaceUri");
        if (ns == null) {
            issues.add(Issue.schemaViolation("body", "namespaceUri", "Property 'namespaceUri' is required.", null));
        } else if (!ns.isString() || !Validation.absoluteUri(ns.asString())) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", "Property 'namespaceUri' must be an absolute URI.", null));
        }
        RequestRules.throwIfAny(issues);
        ExternalService.NamedContent c = service.contentByNamespace(ns.asString());
        return VocabularyController.turtle(c.bytes(), c.name() + ".ttl");
    }

    @PostMapping(path = "/v1/external-vocabulary/searches", consumes = JSON, produces = JSON)
    public Dto.PageDto<Dto.ExternalDto> search(HttpServletRequest request) {
        RequestRules.noQuery(request.getQueryString());
        List<String> uris = searchUris(json.read(request));
        return Dto.PageDto.search(
                service.search(uris).stream().map(Dto.ExternalDto::of).toList());
    }

    /** ExternalVocabularySearchRequest: exactly one oneOf branch, limits of D38, names of D68. */
    static List<String> searchUris(JsonNode body) {
        List<Issue> issues = RequestRules.issues();
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation("body", "body", "The body must be a JSON object.", null));
            RequestRules.throwIfAny(issues);
        }
        RequestRules.unknownProperties(
                body, Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris"), "", issues);
        RequestRules.throwIfAny(issues);
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "body",
                    "Give exactly one of namespaceUri, aliasUri, or namespaceUris and/or aliasUris.",
                    null));
            RequestRules.throwIfAny(issues);
        }
        List<String> uris = new ArrayList<>();
        if (single || alias) {
            String field = single ? "namespaceUri" : "aliasUri";
            JsonNode v = body.get(field);
            if (!v.isString() || !Validation.absoluteUri(v.asString())) {
                issues.add(Issue.schemaViolation("body", field, "'" + field + "' must be an absolute URI.", null));
            } else {
                uris.add(v.asString());
            }
            RequestRules.throwIfAny(issues);
            return uris;
        }
        int total = 0;
        for (String field : List.of("namespaceUris", "aliasUris")) {
            JsonNode arr = body.get(field);
            if (arr == null) {
                continue;
            }
            if (!arr.isArray() || arr.isEmpty()) {
                issues.add(Issue.schemaViolation(
                        "body", field, "'" + field + "' must be an array with at least 1 URI.", null));
                continue;
            }
            total += arr.size();
            for (int i = 0; i < arr.size(); i++) {
                JsonNode v = arr.get(i);
                if (!v.isString() || !Validation.absoluteUri(v.asString())) {
                    issues.add(Issue.schemaViolation(
                            "body", field + "[" + i + "]", "Each URI must be an absolute URI.", null));
                } else {
                    uris.add(v.asString());
                }
            }
        }
        if (total > MAX_SEARCH_URIS) {
            String name = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
            issues.add(Issue.schemaViolation("body", name, "At most 100 URIs in total are allowed.", null));
        }
        RequestRules.throwIfAny(issues);
        return uris;
    }
}

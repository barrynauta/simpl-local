package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.security.CurrentUser;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.RegisterExternalCommand;
import eu.simpl.vocabularymanager.validation.UriKeys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations (D30, D41, D42, D56, D66, D70, D73). */
@RestController
public class ExternalVocabularyController {

    /** Maximum alias URIs (D56). */
    public static final int MAX_ALIASES = 20;

    private static final Set<String> REGISTER_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final Set<String> PAGE_PARAMETERS = Set.of("page", "pageSize");

    private final ExternalVocabularyService service;
    private final Representations representations;
    private final VocabularyManagerProperties.Limits limits;

    /**
     * @param service external vocabulary service
     * @param representations wire representations
     * @param properties component configuration
     */
    public ExternalVocabularyController(
            ExternalVocabularyService service,
            Representations representations,
            VocabularyManagerProperties properties) {
        this.service = service;
        this.representations = representations;
        this.limits = properties.limits();
    }

    /** GET /external-vocabularies: paged, ordered by name (D41). */
    @GetMapping("/v1/external-vocabularies")
    public ResponseEntity<byte[]> list(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        List<Issue> issues = new ArrayList<>();
        Map<String, String> parameters = RequestChecks.query(request, PAGE_PARAMETERS, issues);
        PageRequest page = QueryParsers.page(parameters, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        Page<ExternalVocabulary> result = service.list(page);
        return JsonResponses.json(
                200,
                Representations.page(
                        "/external-vocabularies",
                        parameters,
                        page.page(),
                        page.pageSize(),
                        result.total(),
                        result.items().stream().map(representations::external).toList()));
    }

    /** POST /external-vocabularies: register (D30, D56). */
    @PostMapping("/v1/external-vocabularies")
    public ResponseEntity<byte[]> register(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeMultipart(request);
        MultipartForm form =
                MultipartForm.read(request, "vocabularyFile", limits.maxFileBytes(), limits.maxRequestBytes());
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        issues.addAll(form.unknownFields(REGISTER_FIELDS));
        byte[] content = FieldRules.file(form, issues);
        String name = FieldRules.name(form, issues);
        String namespaceUri = uriField(form, "namespaceUri", true, issues);
        String sourceUri = uriField(form, "sourceUri", false, issues);
        String description = FieldRules.text(form, "description", false, issues);
        List<String> aliases = aliases(form, namespaceUri, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        MDC.put("vocabularyName", name);
        ExternalVocabulary stored = service.register(new RegisterExternalCommand(
                name, namespaceUri, aliases, sourceUri, description, content, CurrentUser.creator()));
        return JsonResponses.created(Representations.externalHref(name), representations.external(stored));
    }

    private static String uriField(MultipartForm form, String field, boolean required, List<Issue> issues) {
        FormPart part = form.single(field, issues);
        if (part == null) {
            if (required && !form.has(field)) {
                issues.add(Issue.schemaViolation("body", field, "Form field '" + field + "' is required.", null));
            }
            return null;
        }
        String value = part.text();
        if (!UriKeys.isAbsolute(value)) {
            issues.add(
                    Issue.schemaViolation("body", field, "Form field '" + field + "' must be an absolute URI.", null));
            return null;
        }
        return value;
    }

    private static List<String> aliases(MultipartForm form, String namespaceUri, List<Issue> issues) {
        List<FormPart> parts = form.all("aliasUris");
        List<String> aliases = new ArrayList<>();
        if (parts.size() > MAX_ALIASES) {
            issues.add(Issue.schemaViolation("body", "aliasUris", "At most 20 alias URIs are allowed.", null));
            return aliases;
        }
        Set<String> keys = new HashSet<>();
        if (namespaceUri != null) {
            keys.add(UriKeys.key(namespaceUri));
        }
        for (FormPart part : parts) {
            String value = part.text();
            if (!UriKeys.isAbsolute(value)) {
                issues.add(Issue.schemaViolation("body", "aliasUris", "Each alias URI must be an absolute URI.", null));
                continue;
            }
            if (!keys.add(UriKeys.key(value))) {
                issues.add(Issue.schemaViolation(
                        "body", "aliasUris", "An alias URI repeats the namespace URI or another alias.", value));
                continue;
            }
            aliases.add(value);
        }
        return aliases;
    }

    /** POST /external-vocabularies/content: content by namespace or alias URI (D66). */
    @PostMapping("/v1/external-vocabularies/content")
    public ResponseEntity<byte[]> contentByNamespace(HttpServletRequest request) {
        RequestChecks.acceptTurtle(request);
        RequestChecks.contentTypeJson(request);
        JsonNode body = JsonBody.readObject(request, limits.maxJsonBytes());
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        SearchController.unknownProperties(body, Set.of("namespaceUri"), "", issues);
        JsonNode uri = body.get("namespaceUri");
        if (uri == null) {
            issues.add(Issue.schemaViolation("body", "namespaceUri", "Property 'namespaceUri' is required.", null));
        } else if (!uri.isString() || !UriKeys.isAbsolute(uri.asString())) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", "Property 'namespaceUri' must be an absolute URI.", null));
        }
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        Map.Entry<ExternalVocabulary, byte[]> found = service.contentByNamespace(uri.asString());
        return JsonResponses.turtle(found.getKey().name() + ".ttl", found.getValue());
    }

    /** GET /external-vocabularies/{name}/content (D41). */
    @GetMapping("/v1/external-vocabularies/{name}/content")
    public ResponseEntity<byte[]> content(HttpServletRequest request, @PathVariable String name) {
        RequestChecks.acceptTurtle(request);
        String checked = checkPath(request, name);
        return JsonResponses.turtle(checked + ".ttl", service.content(checked));
    }

    /** GET /external-vocabularies/{name} (D41). */
    @GetMapping("/v1/external-vocabularies/{name}")
    public ResponseEntity<byte[]> get(HttpServletRequest request, @PathVariable String name) {
        RequestChecks.acceptJson(request);
        String checked = checkPath(request, name);
        return JsonResponses.json(200, representations.external(service.get(checked)));
    }

    /** DELETE /external-vocabularies/{name} (D41, D42). */
    @DeleteMapping("/v1/external-vocabularies/{name}")
    public ResponseEntity<byte[]> delete(HttpServletRequest request, @PathVariable String name) {
        RequestChecks.acceptJson(request);
        String checked = checkPath(request, name);
        MDC.put("vocabularyName", checked);
        service.delete(checked);
        return ResponseEntity.noContent().build();
    }

    private static String checkPath(HttpServletRequest request, String name) {
        List<Issue> issues = new ArrayList<>();
        Issue nameIssue = RequestChecks.nameIssue(name, "path", "name");
        if (nameIssue != null) {
            issues.add(nameIssue);
        }
        RequestChecks.query(request, Set.of(), issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return name;
    }
}

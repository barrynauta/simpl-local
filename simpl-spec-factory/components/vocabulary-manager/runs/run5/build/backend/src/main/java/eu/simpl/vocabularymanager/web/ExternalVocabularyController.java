package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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

/** External vocabulary operations (D30, D38, D41, D42, D56). */
@RestController
@RequestMapping("/v1")
public class ExternalVocabularyController {

    private static final Set<String> REGISTER_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final Set<String> SEARCH_PROPERTIES =
            Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");
    private static final String FILE = "vocabularyFile";
    private static final int MAX_ALIASES = 20;
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalVocabularyService service;
    private final RequestInspector inspector;
    private final Representations representations;
    private final long uploadLimit;

    public ExternalVocabularyController(
            ExternalVocabularyService service,
            RequestInspector inspector,
            Representations representations,
            VocabularyProperties properties) {
        this.service = service;
        this.inspector = inspector;
        this.representations = representations;
        this.uploadLimit = properties.uploadLimitBytes();
    }

    /** GET /external-vocabularies: paged, name ascending, no filters (D41). */
    @GetMapping(path = "/external-vocabularies", produces = MediaType.APPLICATION_JSON_VALUE)
    public CollectionPage<Representations.ExternalBody> list(HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        Map<String, List<String>> params = inspector.query(request);
        List<Issue> issues = inspector.queryIssues(params, Set.of("page", "pageSize"));
        Paging paging = Paging.read(params, issues);
        PathValues.failOn(issues);
        List<Representations.ExternalBody> all =
                service.list().stream().map(representations::external).toList();
        return CollectionPage.of(all, paging.page(), paging.pageSize(), "/external-vocabularies", "");
    }

    /** POST /external-vocabularies: register (D30, D41, D56). */
    @PostMapping("/external-vocabularies")
    public ResponseEntity<Representations.ExternalBody> register(HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireMultipart(request);
        List<FormPart> parts = inspector.multipart(request, FILE, uploadLimit);
        List<Issue> issues = new ArrayList<>(inspector.queryIssues(inspector.query(request), Set.of()));
        FormFields fields = new FormFields(parts, REGISTER_FIELDS, Set.of("aliasUris"));
        byte[] content = fields.requiredFile(FILE);
        String name = fields.requiredName("name");
        String namespaceUri = requiredUri(fields, "namespaceUri");
        List<String> aliases = aliases(fields, namespaceUri);
        String description = fields.optionalText("description");
        String sourceUri = optionalUri(fields, "sourceUri");
        issues.addAll(fields.issues());
        PathValues.failOn(issues);

        ExternalVocabulary stored = service.register(
                new ExternalVocabulary(name, namespaceUri, aliases, sourceUri, description, user.creator(), null),
                content);
        Representations.ExternalBody body = representations.external(stored);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** POST /external-vocabulary/searches (D38). */
    @PostMapping("/external-vocabulary/searches")
    public CollectionPage<Representations.ExternalBody> search(HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireJson(request);
        List<Issue> issues = new ArrayList<>(inspector.queryIssues(inspector.query(request), Set.of()));
        JsonNode body = inspector.jsonObject(request);
        List<String> namespaceUris = new ArrayList<>();
        List<String> aliasUris = new ArrayList<>();
        readSearch(body, namespaceUris, aliasUris, issues);
        PathValues.failOn(issues);
        return CollectionPage.unpaged(service.search(namespaceUris, aliasUris).stream()
                .map(representations::external)
                .toList());
    }

    /** POST /external-vocabularies/content: content by namespace or alias URI. */
    @PostMapping("/external-vocabularies/content")
    public ResponseEntity<byte[]> contentByUri(HttpServletRequest request) {
        inspector.requireAcceptable(request, TurtleResponses.TURTLE);
        inspector.requireJson(request);
        List<Issue> issues = new ArrayList<>(inspector.queryIssues(inspector.query(request), Set.of()));
        JsonNode body = inspector.jsonObject(request);
        Iterator<String> names = body.propertyNames().iterator();
        while (names.hasNext()) {
            String field = names.next();
            if (!"namespaceUri".equals(field)) {
                issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' is not supported.", null));
            }
        }
        JsonNode uri = body.get("namespaceUri");
        if (uri == null || !uri.isString() || !Uris.isAbsoluteUri(uri.asString())) {
            issues.add(Issue.schemaViolation(
                    "body", "namespaceUri", "Property 'namespaceUri' is required and must be an absolute URI.", null));
        }
        PathValues.failOn(issues);
        String namespace = uri.asString();
        ExternalVocabulary found = service.findByUri(namespace)
                .orElseThrow(() -> ApiException.notFound(
                        "External vocabulary with namespaceUri '" + namespace + "' was not found.", null));
        return TurtleResponses.attachment(service.content(found.name()), found.name() + ".ttl");
    }

    /** GET /external-vocabularies/{name}/content. */
    @GetMapping("/external-vocabularies/{name}/content")
    public ResponseEntity<byte[]> content(@PathVariable String name, HttpServletRequest request) {
        inspector.requireAcceptable(request, TurtleResponses.TURTLE);
        checkNameAndQuery(name, request);
        return TurtleResponses.attachment(service.content(name), name + ".ttl");
    }

    /** GET /external-vocabularies/{name}. */
    @GetMapping(path = "/external-vocabularies/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Representations.ExternalBody get(@PathVariable String name, HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        checkNameAndQuery(name, request);
        return representations.external(service.get(name));
    }

    /** DELETE /external-vocabularies/{name} (D41, D42). */
    @DeleteMapping("/external-vocabularies/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name, HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        checkNameAndQuery(name, request);
        service.delete(name, user.subject());
        return ResponseEntity.noContent().build();
    }

    private void checkNameAndQuery(String name, HttpServletRequest request) {
        List<Issue> issues = new ArrayList<>(inspector.queryIssues(inspector.query(request), Set.of()));
        PathValues.checkName("name", name, issues);
        PathValues.failOn(issues);
    }

    private static String requiredUri(FormFields fields, String field) {
        FormPart part = fields.part(field);
        if (part == null) {
            fields.addIssue(Issue.schemaViolation("body", field, "Form field '" + field + "' is required.", null));
            return null;
        }
        String value = part.text().trim();
        if (!Uris.isAbsoluteUri(value)) {
            fields.addIssue(
                    Issue.schemaViolation("body", field, "Form field '" + field + "' must be an absolute URI.", value));
            return null;
        }
        return value;
    }

    private static String optionalUri(FormFields fields, String field) {
        FormPart part = fields.part(field);
        if (part == null || part.text().isBlank()) {
            return null;
        }
        String value = part.text().trim();
        if (!Uris.isAbsoluteUri(value)) {
            fields.addIssue(
                    Issue.schemaViolation("body", field, "Form field '" + field + "' must be an absolute URI.", value));
            return null;
        }
        return value;
    }

    private static List<String> aliases(FormFields fields, String namespaceUri) {
        List<FormPart> parts = fields.parts("aliasUris");
        List<String> aliases = new ArrayList<>();
        if (parts.size() > MAX_ALIASES) {
            fields.addIssue(Issue.schemaViolation(
                    "body", "aliasUris", "At most " + MAX_ALIASES + " alias URIs are allowed.", null));
            return aliases;
        }
        Set<String> seen = new HashSet<>();
        if (namespaceUri != null) {
            seen.add(ExternalVocabulary.normalise(namespaceUri));
        }
        for (FormPart part : parts) {
            String value = part.text().trim();
            if (!Uris.isAbsoluteUri(value)) {
                fields.addIssue(
                        Issue.schemaViolation("body", "aliasUris", "Each alias URI must be an absolute URI.", value));
            } else if (!seen.add(ExternalVocabulary.normalise(value))) {
                fields.addIssue(Issue.schemaViolation(
                        "body",
                        "aliasUris",
                        "An alias URI must differ from the namespace URI and from the other aliases.",
                        value));
            } else {
                aliases.add(value);
            }
        }
        return aliases;
    }

    private static void readSearch(
            JsonNode body, List<String> namespaceUris, List<String> aliasUris, List<Issue> issues) {
        Iterator<String> names = body.propertyNames().iterator();
        while (names.hasNext()) {
            String field = names.next();
            if (!SEARCH_PROPERTIES.contains(field)) {
                issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' is not supported.", null));
            }
        }
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "body",
                    "Give exactly one of: namespaceUri, aliasUri, or namespaceUris and/or aliasUris.",
                    null));
            return;
        }
        if (single) {
            singleUri(body, "namespaceUri", namespaceUris, issues);
        } else if (alias) {
            singleUri(body, "aliasUri", aliasUris, issues);
        } else {
            uriArray(body, "namespaceUris", namespaceUris, issues);
            uriArray(body, "aliasUris", aliasUris, issues);
            if (namespaceUris.size() + aliasUris.size() > MAX_SEARCH_URIS) {
                String name = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                issues.add(Issue.schemaViolation(
                        "body", name, "At most " + MAX_SEARCH_URIS + " URIs are allowed in total.", null));
            }
        }
    }

    private static void singleUri(JsonNode body, String field, List<String> target, List<Issue> issues) {
        JsonNode value = body.get(field);
        if (value == null || !value.isString() || !Uris.isAbsoluteUri(value.asString())) {
            issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' must be an absolute URI.", null));
            return;
        }
        target.add(value.asString());
    }

    private static void uriArray(JsonNode body, String field, List<String> target, List<Issue> issues) {
        JsonNode array = body.get(field);
        if (array == null) {
            return;
        }
        if (!array.isArray() || array.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    "body", field, "Property '" + field + "' must be an array with at least one URI.", null));
            return;
        }
        for (JsonNode value : array) {
            if (!value.isString() || !Uris.isAbsoluteUri(value.asString())) {
                issues.add(Issue.schemaViolation(
                        "body", field, "Every entry of '" + field + "' must be an absolute URI.", null));
                return;
            }
            target.add(value.asString());
        }
    }
}

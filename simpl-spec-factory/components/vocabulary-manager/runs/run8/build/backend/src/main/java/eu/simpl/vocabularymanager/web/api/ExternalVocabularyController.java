package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService.NamedContent;
import eu.simpl.vocabularymanager.web.api.Representations.CollectionPage;
import eu.simpl.vocabularymanager.web.api.Representations.ExternalVocabularyRepresentation;
import eu.simpl.vocabularymanager.web.body.JsonBody;
import eu.simpl.vocabularymanager.web.body.MultipartForm;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.routing.QueryParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** External vocabulary management operations of the OpenAPI (D30, D38, D41, D56, D66, D73). */
@RestController
public class ExternalVocabularyController {

    private static final Set<String> REGISTER_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final Set<String> SEARCH_PROPERTIES = Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalVocabularyService service;
    private final Responses responses;

    /**
     * @param service the external vocabulary service
     * @param responses response builder
     */
    public ExternalVocabularyController(ExternalVocabularyService service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    /**
     * getExternalVocabularies: paged, name ascending only (D41).
     *
     * @param request the request
     * @return the page
     */
    @GetMapping("/external-vocabularies")
    public ResponseEntity<byte[]> getExternalVocabularies(HttpServletRequest request) {
        QueryParameters parameters = QueryParameters.parse(request.getQueryString());
        List<ProblemIssue> issues = new ArrayList<>();
        Paging paging = Paging.parse(parameters, issues);
        Responses.failOn(issues);
        Page<ExternalVocabulary> page = service.list(paging.offset(), paging.pageSize());
        List<ExternalVocabularyRepresentation> items =
                page.items().stream().map(ExternalVocabularyRepresentation::of).toList();
        return responses.ok(paging.page("/external-vocabularies", parameters, page.total(), items));
    }

    /**
     * createExternalVocabulary: register (D30, D56).
     *
     * @param request the multipart request
     * @return 201 with the external vocabulary
     */
    @PostMapping("/external-vocabularies")
    public ResponseEntity<byte[]> createExternalVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request, REGISTER_FIELDS, Set.of("aliasUris"));
        List<ProblemIssue> issues = new ArrayList<>();
        String name = form.text("name");
        if (name == null) {
            issues.add(VocabularyController.required("name"));
        } else if (!FieldRules.isValidName(name)) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "name", "Name must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
        String namespaceUri = form.text("namespaceUri");
        if (namespaceUri == null) {
            issues.add(VocabularyController.required("namespaceUri"));
        } else if (!FieldRules.isAbsoluteUri(namespaceUri)) {
            issues.add(absoluteUri("namespaceUri"));
        }
        List<String> aliases = form.texts("aliasUris");
        aliasIssues(aliases, namespaceUri, issues);
        String sourceUri = form.text("sourceUri");
        if (sourceUri != null && !FieldRules.isAbsoluteUri(sourceUri)) {
            issues.add(absoluteUri("sourceUri"));
        }
        String description = form.text("description");
        if (description != null && !FieldRules.isValidOptionalText(description)) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "description", "description may have at most 1000 characters after trimming."));
        }
        if (!form.has("vocabularyFile")) {
            issues.add(VocabularyController.required("vocabularyFile"));
        }
        Responses.failOn(issues);
        String trimmedDescription =
                description == null || description.isBlank() ? null : description.strip();
        ExternalVocabulary registered = service.register(
                new ExternalVocabularyService.Registration(
                        name, namespaceUri, aliases, sourceUri, trimmedDescription, form.bytes("vocabularyFile")),
                Responses.currentUser());
        ExternalVocabularyRepresentation body = ExternalVocabularyRepresentation.of(registered);
        return responses.created(body, body.href());
    }

    /**
     * searchExternalVocabularies (D38, D68).
     *
     * @param request the JSON request
     * @return the matches, not paged
     */
    @PostMapping("/external-vocabulary/searches")
    public ResponseEntity<byte[]> searchExternalVocabularies(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>(JsonBody.unknownProperties(body, SEARCH_PROPERTIES, ""));
        Responses.failOn(issues);
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "body", "Send exactly one of namespaceUri, aliasUri, or namespaceUris and/or aliasUris."));
            Responses.failOn(issues);
        }
        List<String> uris = new ArrayList<>();
        if (single) {
            singleUri(body, "namespaceUri", uris, issues);
        } else if (alias) {
            singleUri(body, "aliasUri", uris, issues);
        } else {
            uriArray(body, "namespaceUris", uris, issues);
            uriArray(body, "aliasUris", uris, issues);
            if (issues.isEmpty() && uris.size() > MAX_SEARCH_URIS) {
                String field = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                issues.add(ProblemIssue.schemaViolation(
                        "body", field, "At most 100 URIs may be sent in one search."));
            }
        }
        Responses.failOn(issues);
        List<ExternalVocabularyRepresentation> items =
                service.search(uris).stream().map(ExternalVocabularyRepresentation::of).toList();
        return responses.ok(new CollectionPage<>(
                1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, items));
    }

    /**
     * downloadExternalVocabularyContent: lookup by namespace or alias URI (D66).
     *
     * @param request the JSON request
     * @return the Turtle bytes
     */
    @PostMapping("/external-vocabularies/content")
    public ResponseEntity<byte[]> downloadExternalVocabularyContent(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>(JsonBody.unknownProperties(body, Set.of("namespaceUri"), ""));
        List<String> uris = new ArrayList<>();
        if (!body.has("namespaceUri")) {
            issues.add(VocabularyController.required("namespaceUri"));
        } else {
            singleUri(body, "namespaceUri", uris, issues);
        }
        Responses.failOn(issues);
        NamedContent content = service.contentByNamespace(uris.get(0));
        return responses.turtle(content.content(), content.name() + ".ttl");
    }

    /**
     * getExternalVocabularyContentByName.
     *
     * @param name the name
     * @return the Turtle bytes, filename '&lt;Name&gt;.ttl'
     */
    @GetMapping("/external-vocabularies/{name}/content")
    public ResponseEntity<byte[]> getExternalVocabularyContentByName(@PathVariable String name) {
        return responses.turtle(service.content(name), name + ".ttl");
    }

    /**
     * getExternalVocabulary.
     *
     * @param name the name
     * @return the external vocabulary
     */
    @GetMapping("/external-vocabularies/{name}")
    public ResponseEntity<byte[]> getExternalVocabulary(@PathVariable String name) {
        return responses.ok(ExternalVocabularyRepresentation.of(service.get(name)));
    }

    /**
     * deleteExternalVocabulary: hard delete (D41, D42).
     *
     * @param name the name
     * @return 204
     */
    @DeleteMapping("/external-vocabularies/{name}")
    public ResponseEntity<Void> deleteExternalVocabulary(@PathVariable String name) {
        service.delete(name, Responses.currentUser());
        return ResponseEntity.noContent().build();
    }

    private static void aliasIssues(List<String> aliases, String namespaceUri, List<ProblemIssue> issues) {
        if (aliases.size() > FieldRules.MAX_ALIASES) {
            issues.add(ProblemIssue.schemaViolation("body", "aliasUris", "At most 20 aliasUris may be given."));
            return;
        }
        Set<String> seen = new HashSet<>();
        if (namespaceUri != null) {
            seen.add(FieldRules.normalizeNamespace(namespaceUri));
        }
        for (String alias : aliases) {
            if (!FieldRules.isAbsoluteUri(alias)) {
                issues.add(absoluteUri("aliasUris"));
                return;
            }
            if (!seen.add(FieldRules.normalizeNamespace(alias))) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", "aliasUris", "An alias may not repeat the namespace URI or another alias."));
                return;
            }
        }
    }

    private static void singleUri(JsonNode body, String field, List<String> uris, List<ProblemIssue> issues) {
        JsonNode value = body.get(field);
        if (value == null || !value.isString() || !FieldRules.isAbsoluteUri(value.asString())) {
            issues.add(absoluteUri(field));
            return;
        }
        uris.add(value.asString());
    }

    private static void uriArray(JsonNode body, String field, List<String> uris, List<ProblemIssue> issues) {
        JsonNode array = body.get(field);
        if (array == null) {
            return;
        }
        if (!array.isArray() || array.isEmpty()) {
            issues.add(ProblemIssue.schemaViolation("body", field, field + " must be an array with at least 1 URI."));
            return;
        }
        for (JsonNode value : array) {
            if (!value.isString() || !FieldRules.isAbsoluteUri(value.asString())) {
                issues.add(absoluteUri(field));
                return;
            }
            uris.add(value.asString());
        }
    }

    private static ProblemIssue absoluteUri(String field) {
        return ProblemIssue.schemaViolation("body", field, field + " must be an absolute URI.");
    }
}

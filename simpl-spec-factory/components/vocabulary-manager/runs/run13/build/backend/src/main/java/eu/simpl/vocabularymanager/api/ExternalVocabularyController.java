package eu.simpl.vocabularymanager.api;

import static eu.simpl.vocabularymanager.api.ApiSupport.JSON;
import static eu.simpl.vocabularymanager.api.ApiSupport.MULTIPART;
import static eu.simpl.vocabularymanager.api.ApiSupport.PROBLEM_JSON;

import eu.simpl.vocabularymanager.api.dto.CollectionResponse;
import eu.simpl.vocabularymanager.api.dto.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.UriKeys;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService.Registration;
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
import tools.jackson.databind.JsonNode;

/** The external vocabulary operations (D30, D41, D42, D56, D66, D70, D73, D147, D148). */
@RestController
public class ExternalVocabularyController {

    private static final Set<String> REGISTER_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");

    private final ExternalVocabularyService service;

    public ExternalVocabularyController(ExternalVocabularyService service) {
        this.service = service;
    }

    @GetMapping(
            path = "/v1/external-vocabularies",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<CollectionResponse<ExternalVocabularyResponse>> getExternalVocabularies(
            HttpServletRequest request) {
        List<ProblemIssue> issues = new ArrayList<>();
        QueryParameters query = QueryParameters.read(request, ApiSupport.PAGING, issues);
        int[] paging = ApiSupport.paging(query, issues);
        ApiSupport.failOnIssues(issues);
        Page<ExternalVocabulary> page = service.list(paging[0], paging[1]);
        List<ExternalVocabularyResponse> items =
                page.items().stream().map(Representations::external).toList();
        return ApiSupport.json(Representations.page(
                "/external-vocabularies", paging[0], paging[1], page.total(), query.others(), items));
    }

    @PostMapping(
            path = "/v1/external-vocabularies",
            consumes = MULTIPART,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<ExternalVocabularyResponse> createExternalVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        form.checkNames(REGISTER_FIELDS, Set.of("aliasUris"), issues);
        String name = RequestRules.name(form.text("name"), "body", "name", issues);
        String namespaceUri = form.text("namespaceUri");
        if (namespaceUri == null) {
            issues.add(ProblemIssue.schemaViolation("body", "namespaceUri", "'namespaceUri' is required.", null));
        } else if (!RequestRules.absoluteUri(namespaceUri)) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "namespaceUri", "'namespaceUri' must be an absolute URI.", null));
            namespaceUri = null;
        }
        List<String> aliases = aliases(form.texts("aliasUris"), namespaceUri, issues);
        String sourceUri = form.text("sourceUri");
        if (sourceUri != null && !RequestRules.absoluteUri(sourceUri)) {
            issues.add(ProblemIssue.schemaViolation("body", "sourceUri", "'sourceUri' must be an absolute URI.", null));
        }
        String description = RequestRules.text(form.text("description"), "description", false, issues);
        if (!form.has("vocabularyFile")) {
            issues.add(ProblemIssue.schemaViolation("body", "vocabularyFile", "'vocabularyFile' is required.", null));
        }
        ApiSupport.failOnIssues(issues);
        org.slf4j.MDC.put("vocabularyName", name);
        ExternalVocabulary created = service.register(
                new Registration(name, namespaceUri, aliases, sourceUri, description, form.bytes("vocabularyFile")),
                ApiSupport.user(request).creator());
        ExternalVocabularyResponse body = Representations.external(created);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** At most 20 aliases, each absolute, none equal to the namespace or to another alias (D56, D73). */
    private static List<String> aliases(List<String> values, String namespaceUri, List<ProblemIssue> issues) {
        if (values.size() > RequestRules.MAX_ALIASES) {
            issues.add(ProblemIssue.schemaViolation("body", "aliasUris", "At most 20 alias URIs are allowed.", null));
            return List.of();
        }
        Set<String> keys = new HashSet<>();
        if (namespaceUri != null) {
            keys.add(UriKeys.key(namespaceUri));
        }
        for (String alias : values) {
            if (!RequestRules.absoluteUri(alias)) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", "aliasUris", "Each alias URI must be an absolute URI.", null));
                return List.of();
            }
            if (!keys.add(UriKeys.key(alias))) {
                issues.add(ProblemIssue.schemaViolation(
                        "body",
                        "aliasUris",
                        "An alias URI must differ from the namespace URI and from the other aliases.",
                        null));
                return List.of();
            }
        }
        return values;
    }

    @PostMapping(
            path = "/v1/external-vocabularies/content",
            consumes = JSON,
            produces = {ApiSupport.TURTLE, PROBLEM_JSON})
    public ResponseEntity<byte[]> downloadExternalVocabularyContent(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String uri = SearchController.contentRequestUri(body, issues);
        ApiSupport.failOnIssues(issues);
        Map.Entry<ExternalVocabulary, byte[]> found = service.contentByNamespace(uri);
        return ApiSupport.turtle(found.getValue(), found.getKey().name() + ".ttl");
    }

    @GetMapping(path = "/v1/external-vocabularies/{name}/content", produces = ApiSupport.TURTLE)
    public ResponseEntity<byte[]> getExternalVocabularyContentByName(
            HttpServletRequest request, @PathVariable("name") String name) {
        String checked = checkedName(request, name);
        return ApiSupport.turtle(service.content(checked), checked + ".ttl");
    }

    @GetMapping(
            path = "/v1/external-vocabularies/{name}",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<ExternalVocabularyResponse> getExternalVocabulary(
            HttpServletRequest request, @PathVariable("name") String name) {
        String checked = checkedName(request, name);
        return ApiSupport.json(Representations.external(service.get(checked)));
    }

    @DeleteMapping(path = "/v1/external-vocabularies/{name}")
    public ResponseEntity<Void> deleteExternalVocabulary(
            HttpServletRequest request, @PathVariable("name") String name) {
        String checked = checkedName(request, name);
        service.delete(checked);
        return ResponseEntity.noContent().build();
    }

    private static String checkedName(HttpServletRequest request, String name) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String checked = ApiSupport.pathName(name, "name", issues);
        ApiSupport.failOnIssues(issues);
        return checked;
    }
}

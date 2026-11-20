package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.security.CurrentUser;
import eu.simpl.vocabularymanager.service.CreateVersionCommand;
import eu.simpl.vocabularymanager.service.CreateVocabularyCommand;
import eu.simpl.vocabularymanager.service.CreationResult;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Vocabulary and vocabulary version operations of the decided contract. Checks run in the order of D51. */
@RestController
public class VocabularyController {

    private static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMETERS = Set.of("sort", "page", "pageSize");
    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");

    private final VocabularyService service;
    private final Representations representations;
    private final VocabularyManagerProperties.Limits limits;

    /**
     * @param service vocabulary service
     * @param representations wire representations
     * @param properties component configuration
     */
    public VocabularyController(
            VocabularyService service, Representations representations, VocabularyManagerProperties properties) {
        this.service = service;
        this.representations = representations;
        this.limits = properties.limits();
    }

    /** GET /vocabularies (SIMPL-18162, SIMPL-18164). */
    @GetMapping("/v1/vocabularies")
    public ResponseEntity<byte[]> list(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        Map<String, String> parameters = RequestChecks.query(request, LIST_PARAMETERS);
        List<Issue> issues = new ArrayList<>();
        VocabularyFilter filter = QueryParsers.vocabularyFilter(parameters, issues);
        PageRequest page = QueryParsers.page(parameters, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        Page<Vocabulary> result = service.list(filter, page);
        List<Map<String, Object>> items =
                result.items().stream().map(representations::vocabulary).toList();
        return JsonResponses.json(
                200,
                Representations.page("/vocabularies", parameters, page.page(), page.pageSize(), result.total(), items));
    }

    /** POST /vocabularies (SIMPL-18170, SIMPL-18187). */
    @PostMapping("/v1/vocabularies")
    public ResponseEntity<byte[]> create(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeMultipart(request);
        MultipartForm form =
                MultipartForm.read(request, "vocabularyFile", limits.maxFileBytes(), limits.maxRequestBytes());
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        issues.addAll(form.unknownFields(CREATE_FIELDS));
        byte[] content = FieldRules.file(form, issues);
        String name = FieldRules.name(form, issues);
        String description = FieldRules.text(form, "description", true, issues);
        String changelog = FieldRules.text(form, "changelog", true, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        MDC.put("vocabularyName", name);
        CreationResult result = service.create(
                new CreateVocabularyCommand(name, description, changelog, content, CurrentUser.creator()));
        return JsonResponses.created(
                Representations.versionHref(name, result.version().version()), representations.created(result));
    }

    /** GET /vocabularies/{vocabularyName} (SIMPL-18161, SIMPL-18165). */
    @GetMapping("/v1/vocabularies/{vocabularyName}")
    public ResponseEntity<byte[]> get(HttpServletRequest request, @PathVariable String vocabularyName) {
        RequestChecks.acceptJson(request);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.query(request, Set.of(), issues);
        String name = pathName(vocabularyName, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return JsonResponses.json(200, representations.vocabulary(service.get(name)));
    }

    /** PATCH /vocabularies/{vocabularyName} (SIMPL-18175). */
    @PatchMapping("/v1/vocabularies/{vocabularyName}")
    public ResponseEntity<byte[]> changeStatus(HttpServletRequest request, @PathVariable String vocabularyName) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeJson(request);
        JsonNode body = JsonBody.readObject(request, limits.maxJsonBytes());
        List<Issue> issues = new ArrayList<>();
        String name = pathName(vocabularyName, issues);
        RequestChecks.query(request, Set.of(), issues);
        VocabularyStatus status = statusField(body, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        MDC.put("vocabularyName", name);
        return JsonResponses.json(200, representations.vocabulary(service.changeStatus(name, status)));
    }

    /** GET /vocabularies/{vocabularyName}/versions (SIMPL-18168). */
    @GetMapping("/v1/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<byte[]> versions(HttpServletRequest request, @PathVariable String vocabularyName) {
        RequestChecks.acceptJson(request);
        List<Issue> issues = new ArrayList<>();
        String name = pathName(vocabularyName, issues);
        Map<String, String> parameters = RequestChecks.query(request, VERSION_LIST_PARAMETERS, issues);
        QueryParsers.Sort sort = QueryParsers.versionSort(parameters.get("sort"), issues);
        PageRequest page = QueryParsers.page(parameters, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        Page<VocabularyVersion> result = service.versions(name, sort.field(), sort.descending(), page);
        List<Map<String, Object>> items =
                result.items().stream().map(representations::version).toList();
        return JsonResponses.json(
                200,
                Representations.page(
                        "/vocabularies/" + name + "/versions",
                        parameters,
                        page.page(),
                        page.pageSize(),
                        result.total(),
                        items));
    }

    /** POST /vocabularies/{vocabularyName}/versions (SIMPL-18173, SIMPL-18190). */
    @PostMapping("/v1/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<byte[]> createVersion(HttpServletRequest request, @PathVariable String vocabularyName) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeMultipart(request);
        MultipartForm form =
                MultipartForm.read(request, "vocabularyFile", limits.maxFileBytes(), limits.maxRequestBytes());
        List<Issue> issues = new ArrayList<>();
        String name = pathName(vocabularyName, issues);
        RequestChecks.query(request, Set.of(), issues);
        if (form.has("version")) {
            issues.add(Issue.schemaViolation(
                    "body", "version", "The version number is assigned by the API; do not send it.", null));
        }
        List<Issue> unknown = form.unknownFields(VERSION_FIELDS);
        unknown.removeIf(issue -> "version".equals(issue.name()));
        issues.addAll(unknown);
        byte[] content = FieldRules.file(form, issues);
        String description = FieldRules.text(form, "description", true, issues);
        String changelog = FieldRules.text(form, "changelog", true, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        MDC.put("vocabularyName", name);
        CreationResult result = service.createVersion(
                name, new CreateVersionCommand(description, changelog, content, CurrentUser.creator()));
        return JsonResponses.created(
                Representations.versionHref(name, result.version().version()), representations.created(result));
    }

    /** GET /vocabularies/{vocabularyName}/versions/{version} (SIMPL-18166). */
    @GetMapping("/v1/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> version(
            HttpServletRequest request, @PathVariable String vocabularyName, @PathVariable String version) {
        RequestChecks.acceptJson(request);
        List<Issue> issues = new ArrayList<>();
        String name = pathName(vocabularyName, issues);
        Integer number = pathVersion(version, issues);
        RequestChecks.query(request, Set.of(), issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return JsonResponses.json(200, representations.version(service.version(name, number)));
    }

    /** PATCH /vocabularies/{vocabularyName}/versions/{version} (D4, D41). */
    @PatchMapping("/v1/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> changeVersionStatus(
            HttpServletRequest request, @PathVariable String vocabularyName, @PathVariable String version) {
        RequestChecks.acceptJson(request);
        RequestChecks.contentTypeJson(request);
        JsonNode body = JsonBody.readObject(request, limits.maxJsonBytes());
        List<Issue> issues = new ArrayList<>();
        String name = pathName(vocabularyName, issues);
        Integer number = pathVersion(version, issues);
        RequestChecks.query(request, Set.of(), issues);
        VocabularyStatus status = statusField(body, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        MDC.put("vocabularyName", name);
        MDC.put("version", Integer.toString(number));
        return JsonResponses.json(200, representations.version(service.changeVersionStatus(name, number, status)));
    }

    /** The status property of VocabularyStatusPatchRequest; other properties are rejected (D52). */
    static VocabularyStatus statusField(JsonNode body, List<Issue> issues) {
        Iterator<String> names = body.propertyNames().iterator();
        while (names.hasNext()) {
            String property = names.next();
            if (!"status".equals(property)) {
                issues.add(
                        Issue.schemaViolation("body", property, "Property '" + property + "' is not allowed.", null));
            }
        }
        JsonNode status = body.get("status");
        if (status == null) {
            issues.add(Issue.schemaViolation("body", "status", "Property 'status' is required.", null));
            return null;
        }
        if (!status.isString()) {
            issues.add(
                    Issue.schemaViolation("body", "status", "Property 'status' must be ACTIVE or DEPRECATED.", null));
            return null;
        }
        return VocabularyStatus.fromWire(status.asString()).orElseGet(() -> {
            issues.add(Issue.schemaViolation(
                    "body", "status", "Property 'status' must be ACTIVE or DEPRECATED.", status.asString()));
            return null;
        });
    }

    static String pathName(String value, List<Issue> issues) {
        Issue issue = RequestChecks.nameIssue(value, "path", "vocabularyName");
        if (issue != null) {
            issues.add(issue);
            return null;
        }
        return value;
    }

    static Integer pathVersion(String value, List<Issue> issues) {
        Integer number = RequestChecks.positiveInt(value);
        if (number == null) {
            issues.add(Issue.schemaViolation(
                    "path", "version", "Path parameter 'version' must be an integer from 1 to 2147483647.", value));
        }
        return number;
    }
}

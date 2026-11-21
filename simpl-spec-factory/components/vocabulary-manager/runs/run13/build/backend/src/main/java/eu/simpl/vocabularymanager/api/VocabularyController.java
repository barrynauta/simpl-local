package eu.simpl.vocabularymanager.api;

import static eu.simpl.vocabularymanager.api.ApiSupport.JSON;
import static eu.simpl.vocabularymanager.api.ApiSupport.MULTIPART;
import static eu.simpl.vocabularymanager.api.ApiSupport.PROBLEM_JSON;

import eu.simpl.vocabularymanager.api.dto.CollectionResponse;
import eu.simpl.vocabularymanager.api.dto.VersionCreatedResponse;
import eu.simpl.vocabularymanager.api.dto.VersionResponse;
import eu.simpl.vocabularymanager.api.dto.VocabularyResponse;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService.Created;
import eu.simpl.vocabularymanager.service.VocabularyService.Submission;
import eu.simpl.vocabularymanager.service.VocabularyService.VersionView;
import eu.simpl.vocabularymanager.store.VocabularyQuery;
import eu.simpl.vocabularymanager.store.VocabularyQuery.SortField;
import eu.simpl.vocabularymanager.store.VocabularyRepository.VersionSort;
import io.github.resilience4j.bulkhead.Bulkhead;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** The vocabulary operations of the decided contract (stories SIMPL-18161 to SIMPL-18196). */
@RestController
public class VocabularyController {

    private static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMETERS = Set.of("sort", "page", "pageSize");
    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    private static final List<String> LIST_SORTS = List.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    private static final List<String> VERSION_SORTS =
            List.of("name", "-name", "status", "-status", "creationDate", "-creationDate");

    private final VocabularyService service;
    private final Representations representations;
    private final Bulkhead uploads;

    public VocabularyController(VocabularyService service, Representations representations, Bulkhead uploads) {
        this.service = service;
        this.representations = representations;
        this.uploads = uploads;
    }

    @GetMapping(
            path = "/v1/vocabularies",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<CollectionResponse<VocabularyResponse>> getVocabularies(HttpServletRequest request) {
        List<ProblemIssue> issues = new ArrayList<>();
        QueryParameters query = QueryParameters.read(request, LIST_PARAMETERS, issues);
        String name = query.get("name") == null ? null : RequestRules.nameFilter(query.get("name"), issues);
        VocabularyStatus status = query.get("status") == null ? null : status(query.get("status"), "query", issues);
        VocabularyQuery.TimeRange creation = query.get("creationDate") == null
                ? null
                : RequestRules.dateFilter(query.get("creationDate"), "creationDate", issues);
        VocabularyQuery.TimeRange updated = query.get("lastUpdated") == null
                ? null
                : RequestRules.dateFilter(query.get("lastUpdated"), "lastUpdated", issues);
        String sort = query.get("sort") == null ? "name" : sort(query.get("sort"), LIST_SORTS, issues);
        int[] paging = ApiSupport.paging(query, issues);
        ApiSupport.failOnIssues(issues);
        boolean descending = sort.startsWith("-");
        SortField field = switch (descending ? sort.substring(1) : sort) {
            case "status" -> SortField.STATUS;
            case "creationDate" -> SortField.CREATION_DATE;
            case "lastUpdated" -> SortField.LAST_UPDATED;
            default -> SortField.NAME;
        };
        VocabularyQuery criteria = new VocabularyQuery(
                name == null ? null : name.toLowerCase(Locale.ROOT),
                status,
                creation,
                updated,
                field,
                descending,
                paging[0],
                paging[1]);
        Page<Vocabulary> page = service.list(criteria);
        List<VocabularyResponse> items =
                page.items().stream().map(representations::vocabulary).toList();
        return ApiSupport.json(
                Representations.page("/vocabularies", paging[0], paging[1], page.total(), query.others(), items));
    }

    @PostMapping(
            path = "/v1/vocabularies",
            consumes = MULTIPART,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VersionCreatedResponse> createVocabulary(HttpServletRequest request) {
        return limited(() -> {
            MultipartForm form = MultipartForm.read(request);
            List<ProblemIssue> issues = new ArrayList<>();
            ApiSupport.noQuery(request, issues);
            form.checkNames(CREATE_FIELDS, Set.of(), issues);
            String name = RequestRules.name(form.text("name"), "body", "name", issues);
            String description = RequestRules.text(form.text("description"), "description", true, issues);
            String changelog = RequestRules.text(form.text("changelog"), "changelog", true, issues);
            requireFile(form, issues);
            ApiSupport.failOnIssues(issues);
            org.slf4j.MDC.put("vocabularyName", name);
            Created created = service.create(
                    new Submission(name, description, changelog, form.bytes("vocabularyFile")),
                    ApiSupport.user(request));
            return createdResponse(created);
        });
    }

    @GetMapping(
            path = "/v1/vocabularies/{vocabularyName}",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VocabularyResponse> getVocabulary(
            HttpServletRequest request, @PathVariable("vocabularyName") String vocabularyName) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        ApiSupport.failOnIssues(issues);
        return ApiSupport.json(representations.vocabulary(service.get(name)));
    }

    @PatchMapping(
            path = "/v1/vocabularies/{vocabularyName}",
            consumes = JSON,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VocabularyResponse> updateVocabularyStatus(
            HttpServletRequest request, @PathVariable("vocabularyName") String vocabularyName) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        VocabularyStatus target = statusBody(body, issues);
        ApiSupport.failOnIssues(issues);
        return ApiSupport.json(representations.vocabulary(service.setStatus(name, target)));
    }

    @GetMapping(
            path = "/v1/vocabularies/{vocabularyName}/versions",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<CollectionResponse<VersionResponse>> getVocabularyVersions(
            HttpServletRequest request, @PathVariable("vocabularyName") String vocabularyName) {
        List<ProblemIssue> issues = new ArrayList<>();
        QueryParameters query = QueryParameters.read(request, VERSION_LIST_PARAMETERS, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        String sort = query.get("sort") == null ? null : sort(query.get("sort"), VERSION_SORTS, issues);
        int[] paging = ApiSupport.paging(query, issues);
        ApiSupport.failOnIssues(issues);
        Vocabulary vocabulary = service.get(name);
        boolean descending = sort != null && sort.startsWith("-");
        VersionSort field = sort == null
                ? VersionSort.VERSION
                : switch (descending ? sort.substring(1) : sort) {
                    case "status" -> VersionSort.STATUS;
                    case "creationDate" -> VersionSort.CREATION_DATE;
                    default -> VersionSort.VERSION;
                };
        Page<VocabularyVersion> page = service.versions(vocabulary, field, descending, paging[0], paging[1]);
        List<VersionResponse> items = page.items().stream()
                .map(version -> representations.version(version, vocabulary.latestVersion()))
                .toList();
        return ApiSupport.json(Representations.page(
                "/vocabularies/" + name + "/versions", paging[0], paging[1], page.total(), query.others(), items));
    }

    @PostMapping(
            path = "/v1/vocabularies/{vocabularyName}/versions",
            consumes = MULTIPART,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VersionCreatedResponse> createVocabularyVersion(
            HttpServletRequest request, @PathVariable("vocabularyName") String vocabularyName) {
        return limited(() -> {
            MultipartForm form = MultipartForm.read(request);
            List<ProblemIssue> issues = new ArrayList<>();
            ApiSupport.noQuery(request, issues);
            String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
            form.checkNames(VERSION_FIELDS, Set.of(), issues);
            String description = RequestRules.text(form.text("description"), "description", true, issues);
            String changelog = RequestRules.text(form.text("changelog"), "changelog", true, issues);
            requireFile(form, issues);
            ApiSupport.failOnIssues(issues);
            Created created = service.createVersion(
                    new Submission(name, description, changelog, form.bytes("vocabularyFile")),
                    ApiSupport.user(request));
            return createdResponse(created);
        });
    }

    @GetMapping(
            path = "/v1/vocabularies/{vocabularyName}/versions/{version}",
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VersionResponse> getVocabularyVersion(
            HttpServletRequest request,
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        Integer number = ApiSupport.pathVersion(version, issues);
        ApiSupport.failOnIssues(issues);
        VersionView view = service.version(name, number);
        return ApiSupport.json(representations.version(view.version(), view.latestVersion()));
    }

    @PatchMapping(
            path = "/v1/vocabularies/{vocabularyName}/versions/{version}",
            consumes = JSON,
            produces = {JSON, PROBLEM_JSON})
    public ResponseEntity<VersionResponse> updateVocabularyVersionStatus(
            HttpServletRequest request,
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        Integer number = ApiSupport.pathVersion(version, issues);
        VocabularyStatus target = statusBody(body, issues);
        ApiSupport.failOnIssues(issues);
        VersionView view = service.setVersionStatus(name, number, target);
        return ApiSupport.json(representations.version(view.version(), view.latestVersion()));
    }

    @GetMapping(path = "/v1/content/{vocabularyName}", produces = ApiSupport.TURTLE)
    public ResponseEntity<byte[]> downloadVocabulary(
            HttpServletRequest request, @PathVariable("vocabularyName") String vocabularyName) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        ApiSupport.failOnIssues(issues);
        return ApiSupport.turtle(service.latestContent(name), name + ".ttl");
    }

    @GetMapping(path = "/v1/content/{vocabularyName}/versions/{version}", produces = ApiSupport.TURTLE)
    public ResponseEntity<byte[]> downloadVocabularyVersion(
            HttpServletRequest request,
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        String name = ApiSupport.pathName(vocabularyName, "vocabularyName", issues);
        Integer number = ApiSupport.pathVersion(version, issues);
        ApiSupport.failOnIssues(issues);
        return ApiSupport.turtle(service.content(name, number), name + "-v" + number + ".ttl");
    }

    private ResponseEntity<VersionCreatedResponse> createdResponse(Created created) {
        VersionCreatedResponse body = representations.created(created.version(), created.warnings());
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** At most a few uploads per instance at a time (D92). */
    private <T> T limited(Supplier<T> upload) {
        if (!uploads.tryAcquirePermission()) {
            throw Problems.tooManyUploads();
        }
        try {
            return upload.get();
        } finally {
            uploads.onComplete();
        }
    }

    private static void requireFile(MultipartForm form, List<ProblemIssue> issues) {
        if (!form.has("vocabularyFile")) {
            issues.add(ProblemIssue.schemaViolation("body", "vocabularyFile", "'vocabularyFile' is required.", null));
        }
    }

    private static VocabularyStatus statusBody(JsonNode body, List<ProblemIssue> issues) {
        JsonBody.unknownProperties(body, Set.of("status"), "", issues);
        JsonNode status = body.get("status");
        if (status == null || status.isNull()) {
            issues.add(ProblemIssue.schemaViolation("body", "status", "'status' is required.", null));
            return null;
        }
        if (!status.isString()) {
            issues.add(ProblemIssue.schemaViolation("body", "status", "'status' must be ACTIVE or DEPRECATED.", null));
            return null;
        }
        return status(status.asString(), "body", issues);
    }

    private static VocabularyStatus status(String value, String in, List<ProblemIssue> issues) {
        VocabularyStatus status = VocabularyStatus.parse(value);
        if (status == null) {
            issues.add(ProblemIssue.schemaViolation(
                    in, "status", "'status' must be ACTIVE or DEPRECATED.", RequestRules.shorten(value)));
        }
        return status;
    }

    private static String sort(String value, List<String> allowed, List<ProblemIssue> issues) {
        if (!allowed.contains(value)) {
            issues.add(ProblemIssue.schemaViolation(
                    "query",
                    "sort",
                    "Query parameter 'sort' must be one of " + allowed + ".",
                    RequestRules.shorten(value)));
            return null;
        }
        return value;
    }
}

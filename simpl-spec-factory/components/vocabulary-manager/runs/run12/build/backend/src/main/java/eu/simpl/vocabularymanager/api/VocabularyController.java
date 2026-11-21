package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.Model;
import eu.simpl.vocabularymanager.domain.Model.Page;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.UploadLimiter;
import eu.simpl.vocabularymanager.domain.VocabularyService;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.store.VocabularyStore.ListFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Vocabulary and vocabulary version operations (SIMPL-18162 to SIMPL-18196, D3 to D61). */
@RestController
public class VocabularyController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String MULTIPART = MediaType.MULTIPART_FORM_DATA_VALUE;
    private static final String TURTLE = "text/turtle";
    public static final Set<String> LIST_PARAMS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    public static final Set<String> LIST_SORTS = Set.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    public static final Set<String> VERSION_PARAMS = Set.of("sort", "page", "pageSize");
    public static final Set<String> VERSION_SORTS =
            Set.of("name", "-name", "status", "-status", "creationDate", "-creationDate");
    public static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    public static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");

    private final VocabularyService service;
    private final UploadLimiter limiter;
    private final JsonBody json;
    private final String contentBase;

    public VocabularyController(VocabularyService service, UploadLimiter limiter, JsonBody json, AppProperties props) {
        this.service = service;
        this.limiter = limiter;
        this.json = json;
        this.contentBase = props.contentBaseUrl();
    }

    @GetMapping(path = "/v1/vocabularies", produces = JSON)
    public Dto.PageDto<Dto.VocabularyDto> list(HttpServletRequest request) {
        QueryParams q = QueryParams.parse(request.getQueryString(), LIST_PARAMS);
        List<Issue> issues = RequestRules.issues();
        String name = q.get("name");
        if (name != null
                && (name.length() > 64 || !Validation.NAME_FILTER.matcher(name).matches())) {
            issues.add(Issue.schemaViolation(
                    "query",
                    "name",
                    "Query parameter 'name' must be 1 to 64 characters matching ^[A-Za-z0-9]+$.",
                    name));
        }
        String statusText = q.get("status");
        Status status = statusText == null ? null : Status.parse(statusText);
        if (statusText != null && status == null) {
            issues.add(Issue.schemaViolation(
                    "query", "status", "Query parameter 'status' must be one of [ACTIVE, DEPRECATED].", statusText));
        }
        Instant[] created = dateParam(q, "creationDate", issues);
        Instant[] updated = dateParam(q, "lastUpdated", issues);
        String sort = q.get("sort");
        if (sort != null && !LIST_SORTS.contains(sort)) {
            issues.add(Issue.schemaViolation(
                    "query",
                    "sort",
                    "Query parameter 'sort' must be one of [name, -name, status, -status, creationDate,"
                            + " -creationDate, lastUpdated, -lastUpdated].",
                    sort));
        }
        int page = q.positiveInt("page", 1, Integer.MAX_VALUE, issues);
        int pageSize = q.positiveInt("pageSize", 20, 100, issues);
        RequestRules.throwIfAny(issues);
        ListFilter filter = new ListFilter(
                name == null ? null : name.toLowerCase(Locale.ROOT),
                status,
                created == null ? null : created[0],
                created == null ? null : created[1],
                updated == null ? null : updated[0],
                updated == null ? null : updated[1]);
        Page<Model.Vocabulary> result = service.list(filter, sort, page, pageSize);
        List<Dto.VocabularyDto> items = result.items().stream()
                .map(v -> Dto.VocabularyDto.of(v, contentBase))
                .toList();
        return Dto.PageDto.paged("/vocabularies", q.all(), page, pageSize, result.total(), items);
    }

    private static Instant[] dateParam(QueryParams q, String name, List<Issue> issues) {
        String v = q.get(name);
        if (v == null) {
            return null;
        }
        Instant[] range = Validation.dateRange(v);
        if (range == null) {
            issues.add(Issue.schemaViolation(
                    "query",
                    name,
                    "Query parameter '" + name + "' must be a valid date (YYYY-MM-DD) or date-time.",
                    v));
        }
        return range;
    }

    @PostMapping(path = "/v1/vocabularies", consumes = MULTIPART, produces = JSON)
    public ResponseEntity<Dto.VersionDto> create(HttpServletRequest request) {
        try (UploadLimiter.Permit permit = limiter.acquire()) {
            MultipartForm form = MultipartForm.read(request);
            RequestRules.noQuery(request.getQueryString());
            RequestRules.throwIfAny(form.shapeIssues(CREATE_FIELDS, Set.of()));
            List<Issue> issues = RequestRules.issues();
            RequestRules.name(form.text("name"), form.has("name"), issues);
            RequestRules.requiredText(form, "description", issues);
            RequestRules.requiredText(form, "changelog", issues);
            RequestRules.requiredFile(form, issues);
            RequestRules.throwIfAny(issues);
            VocabularyService.Created c = service.create(
                    form.text("name"),
                    form.text("description"),
                    form.text("changelog"),
                    form.file(),
                    RequestRules.user().creator());
            return created(c);
        }
    }

    private ResponseEntity<Dto.VersionDto> created(VocabularyService.Created c) {
        List<Dto.WarningDto> warnings =
                c.warnings().stream().map(Dto.WarningDto::of).toList();
        Dto.VersionDto body = Dto.VersionDto.of(c.version(), contentBase, warnings);
        return ResponseEntity.created(URI.create(body.href())).body(body);
    }

    @GetMapping(path = "/v1/vocabularies/{vocabularyName}", produces = JSON)
    public Dto.VocabularyDto get(@PathVariable("vocabularyName") String name, HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return Dto.VocabularyDto.of(service.get(name), contentBase);
    }

    @PatchMapping(path = "/v1/vocabularies/{vocabularyName}", consumes = JSON, produces = JSON)
    public Dto.VocabularyDto changeStatus(@PathVariable("vocabularyName") String name, HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        RequestRules.noQuery(request.getQueryString());
        Status to = statusBody(json.read(request));
        return Dto.VocabularyDto.of(service.changeStatus(name, to), contentBase);
    }

    /** VocabularyStatusPatchRequest: only 'status', ACTIVE or DEPRECATED (D4, D52). */
    static Status statusBody(JsonNode body) {
        List<Issue> issues = RequestRules.issues();
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation("body", "body", "The body must be a JSON object.", null));
            RequestRules.throwIfAny(issues);
        }
        RequestRules.unknownProperties(body, Set.of("status"), "", issues);
        JsonNode s = body.get("status");
        Status status = null;
        if (s == null) {
            issues.add(Issue.schemaViolation("body", "status", "Property 'status' is required.", null));
        } else {
            status = s.isString() ? Status.parse(s.asString()) : null;
            if (status == null) {
                issues.add(Issue.schemaViolation(
                        "body", "status", "Property 'status' must be one of [ACTIVE, DEPRECATED].", null));
            }
        }
        RequestRules.throwIfAny(issues);
        return status;
    }

    @GetMapping(path = "/v1/vocabularies/{vocabularyName}/versions", produces = JSON)
    public Dto.PageDto<Dto.VersionDto> versions(
            @PathVariable("vocabularyName") String name, HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        QueryParams q = QueryParams.parse(request.getQueryString(), VERSION_PARAMS);
        List<Issue> issues = RequestRules.issues();
        String sort = q.get("sort");
        if (sort != null && !VERSION_SORTS.contains(sort)) {
            issues.add(Issue.schemaViolation(
                    "query",
                    "sort",
                    "Query parameter 'sort' must be one of [name, -name, status, -status, creationDate,"
                            + " -creationDate].",
                    sort));
        }
        int page = q.positiveInt("page", 1, Integer.MAX_VALUE, issues);
        int pageSize = q.positiveInt("pageSize", 20, 100, issues);
        RequestRules.throwIfAny(issues);
        Page<Model.VocabularyVersion> result = service.versions(name, sort, page, pageSize);
        List<Dto.VersionDto> items = result.items().stream()
                .map(v -> Dto.VersionDto.of(v, contentBase, null))
                .toList();
        return Dto.PageDto.paged("/vocabularies/" + name + "/versions", q.all(), page, pageSize, result.total(), items);
    }

    @PostMapping(path = "/v1/vocabularies/{vocabularyName}/versions", consumes = MULTIPART, produces = JSON)
    public ResponseEntity<Dto.VersionDto> createVersion(
            @PathVariable("vocabularyName") String name, HttpServletRequest request) {
        try (UploadLimiter.Permit permit = limiter.acquire()) {
            MultipartForm form = MultipartForm.read(request);
            Validation.pathName("vocabularyName", name);
            RequestRules.noQuery(request.getQueryString());
            RequestRules.throwIfAny(form.shapeIssues(VERSION_FIELDS, Set.of()));
            List<Issue> issues = RequestRules.issues();
            RequestRules.requiredText(form, "description", issues);
            RequestRules.requiredText(form, "changelog", issues);
            RequestRules.requiredFile(form, issues);
            RequestRules.throwIfAny(issues);
            return created(service.createVersion(
                    name,
                    form.text("description"),
                    form.text("changelog"),
                    form.file(),
                    RequestRules.user().creator()));
        }
    }

    @GetMapping(path = "/v1/vocabularies/{vocabularyName}/versions/{version}", produces = JSON)
    public Dto.VersionDto version(
            @PathVariable("vocabularyName") String name,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        int v = Validation.pathVersion(version);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return Dto.VersionDto.of(service.getVersion(name, v), contentBase, null);
    }

    @PatchMapping(path = "/v1/vocabularies/{vocabularyName}/versions/{version}", consumes = JSON, produces = JSON)
    public Dto.VersionDto changeVersionStatus(
            @PathVariable("vocabularyName") String name,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        int v = Validation.pathVersion(version);
        RequestRules.noQuery(request.getQueryString());
        Status to = statusBody(json.read(request));
        return Dto.VersionDto.of(service.changeVersionStatus(name, v, to), contentBase, null);
    }

    @GetMapping(path = "/v1/content/{vocabularyName}", produces = TURTLE)
    public ResponseEntity<byte[]> download(@PathVariable("vocabularyName") String name, HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return turtle(service.latestContent(name), name + ".ttl");
    }

    @GetMapping(path = "/v1/content/{vocabularyName}/versions/{version}", produces = TURTLE)
    public ResponseEntity<byte[]> downloadVersion(
            @PathVariable("vocabularyName") String name,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        Validation.pathName("vocabularyName", name);
        int v = Validation.pathVersion(version);
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        return turtle(service.content(name, v), name + "-v" + v + ".ttl");
    }

    /** text/turtle without charset, bytes as stored, attachment filename (D29, D70). */
    static ResponseEntity<byte[]> turtle(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, TURTLE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}

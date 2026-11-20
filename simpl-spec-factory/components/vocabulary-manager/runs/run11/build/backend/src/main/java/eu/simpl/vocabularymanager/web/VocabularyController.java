package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.UploadLimiter;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.ListQuery;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.validation.DateFilters;
import eu.simpl.vocabularymanager.validation.Names;
import eu.simpl.vocabularymanager.validation.QueryParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Operations on vocabularies and vocabulary versions (OpenAPI tag "Vocabulary management"). */
@RestController
public class VocabularyController {

    static final String JSON = "application/json";
    static final String MULTIPART = "multipart/form-data";
    private static final Set<String> LIST_SORTS = Set.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    private static final Set<String> VERSION_SORTS =
            Set.of("name", "-name", "status", "-status", "creationDate", "-creationDate");
    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");

    private final VocabularyService service;
    private final Representations representations;
    private final UploadLimiter limiter;
    private final JsonBody jsonBody;

    public VocabularyController(
            VocabularyService service, Representations representations, UploadLimiter limiter, JsonBody jsonBody) {
        this.service = service;
        this.representations = representations;
        this.limiter = limiter;
        this.jsonBody = jsonBody;
    }

    @GetMapping(value = "/v1/vocabularies", produces = JSON)
    public Map<String, Object> getVocabularies(HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(
                request.getQueryString(),
                ApiRoutes.find("GET", "/vocabularies").orElseThrow().queryParameters());
        RequestChecks checks = RequestChecks.start();
        String name = query.get("name");
        if (name != null && !Names.isNameFilter(name)) {
            checks.add(Issue.schema(
                    "query", "name", "Query parameter 'name' must match ^[A-Za-z0-9]+$ (1 to 64 characters)."));
        }
        VocabularyStatus status = null;
        if (query.get("status") != null) {
            status = VocabularyStatus.parse(query.get("status")).orElse(null);
            if (status == null) {
                checks.add(Issue.schema(
                        "query", "status", "Query parameter 'status' must be one of [ACTIVE, DEPRECATED]."));
            }
        }
        ListQuery.TimeRange created = dateFilter(query, "creationDate", checks);
        ListQuery.TimeRange updated = dateFilter(query, "lastUpdated", checks);
        String sort = query.get("sort");
        if (sort != null && !LIST_SORTS.contains(sort)) {
            checks.add(Issue.schema("query", "sort", "Query parameter 'sort' must be one of " + LIST_SORTS + "."));
        }
        long page = paging(query, "page", Integer.MAX_VALUE, 1, checks);
        int size = (int) paging(query, "pageSize", 100, 20, checks);
        checks.throwIfAny();
        String field = sort == null ? "name" : sort.replace("-", "");
        boolean ascending = sort == null || !sort.startsWith("-");
        Page<Vocabulary> result =
                service.list(new ListQuery(name, status, created, updated, field, ascending, page, size));
        return representations.page(
                "/vocabularies",
                query.inOrder(),
                page,
                size,
                result.total(),
                result.items().stream().map(representations::vocabulary).toList());
    }

    @PostMapping(value = "/v1/vocabularies", produces = JSON, consumes = MULTIPART)
    public ResponseEntity<Map<String, Object>> createVocabulary(
            HttpServletRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        try (UploadLimiter.Permit permit = limiter.acquire()) {
            MultipartForm form = MultipartForm.read(request);
            RequestChecks checks = RequestChecks.start().noQuery(request.getQueryString());
            checks.addAll(form.unknownFields(CREATE_FIELDS)).addAll(form.repeatedFields(CREATE_FIELDS));
            String name = checks.name("name", form.text("name"));
            String description = checks.requiredText("description", form.text("description"));
            String changelog = checks.requiredText("changelog", form.text("changelog"));
            byte[] content = requireFile(form, checks);
            checks.throwIfAny();
            org.slf4j.MDC.put("vocabularyName", name);
            VocabularyService.Created created = service.create(
                    new VocabularyService.Submission(name, description, changelog, content, user.creator()));
            return createdResponse(created);
        }
    }

    @PostMapping(value = "/v1/vocabularies/searches", produces = JSON, consumes = JSON)
    public Map<String, Object> searchVocabularies(HttpServletRequest request) {
        RequestChecks checks = RequestChecks.start().noQuery(request.getQueryString());
        checks.throwIfAny();
        JsonNode body = jsonBody.readObject(request);
        List<SearchReference> references = SearchRequests.internal(body);
        List<Map<String, Object>> items = new ArrayList<>();
        ReferenceState state = service.searchState(
                references.stream().map(SearchReference::name).distinct().toList());
        for (SearchReference ref : references) {
            if (ref.version() == null) {
                state.versions().stream()
                        .filter(r -> r.name().equals(ref.name()))
                        .findFirst()
                        .ifPresent(r -> items.add(
                                representations.searchItem(r.name(), r.latestVersion(), r.vocabularyStatus(), true)));
            } else {
                state.version(ref.name(), ref.version())
                        .ifPresent(r ->
                                items.add(representations.searchItem(r.name(), r.version(), r.versionStatus(), false)));
            }
        }
        return representations.searchResult(items);
    }

    @GetMapping(value = "/v1/vocabularies/{vocabularyName}", produces = JSON)
    public Map<String, Object> getVocabulary(
            @PathVariable("vocabularyName") String vocabularyName, HttpServletRequest request) {
        RequestChecks.start()
                .pathName("vocabularyName", vocabularyName)
                .noQuery(request.getQueryString())
                .throwIfAny();
        return representations.vocabulary(service.vocabulary(vocabularyName));
    }

    @PatchMapping(value = "/v1/vocabularies/{vocabularyName}", produces = JSON, consumes = JSON)
    public Map<String, Object> updateVocabularyStatus(
            @PathVariable("vocabularyName") String vocabularyName, HttpServletRequest request) {
        RequestChecks checks =
                RequestChecks.start().pathName("vocabularyName", vocabularyName).noQuery(request.getQueryString());
        JsonNode body = readBodyAfter(checks, request);
        VocabularyStatus target = checks.status(body);
        checks.throwIfAny();
        return representations.vocabulary(service.setStatus(vocabularyName, target));
    }

    @GetMapping(value = "/v1/vocabularies/{vocabularyName}/versions", produces = JSON)
    public Map<String, Object> getVocabularyVersions(
            @PathVariable("vocabularyName") String vocabularyName, HttpServletRequest request) {
        RequestChecks checks = RequestChecks.start().pathName("vocabularyName", vocabularyName);
        QueryParameters query;
        try {
            query = QueryParameters.parse(request.getQueryString(), Set.of("sort", "page", "pageSize"));
        } catch (ApiException ex) {
            checks.addAll(ex.issues()).throwIfAny();
            throw ex;
        }
        String sort = query.get("sort");
        if (sort != null && !VERSION_SORTS.contains(sort)) {
            checks.add(Issue.schema("query", "sort", "Query parameter 'sort' must be one of " + VERSION_SORTS + "."));
        }
        long page = paging(query, "page", Integer.MAX_VALUE, 1, checks);
        int size = (int) paging(query, "pageSize", 100, 20, checks);
        checks.throwIfAny();
        String field = sort == null ? null : sort.replace("-", "");
        boolean ascending = sort == null || !sort.startsWith("-");
        Page<VocabularyVersion> result = service.versions(vocabularyName, field, ascending, page, size);
        return representations.page(
                "/vocabularies/" + vocabularyName + "/versions",
                query.inOrder(),
                page,
                size,
                result.total(),
                result.items().stream().map(representations::version).toList());
    }

    @PostMapping(value = "/v1/vocabularies/{vocabularyName}/versions", produces = JSON, consumes = MULTIPART)
    public ResponseEntity<Map<String, Object>> createVocabularyVersion(
            @PathVariable("vocabularyName") String vocabularyName,
            HttpServletRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try (UploadLimiter.Permit permit = limiter.acquire()) {
            MultipartForm form = MultipartForm.read(request);
            RequestChecks checks = RequestChecks.start()
                    .pathName("vocabularyName", vocabularyName)
                    .noQuery(request.getQueryString());
            if (form.names().contains("version")) {
                checks.add(Issue.schema(
                        "body", "version", "The version number is assigned by the system; do not send it."));
            }
            List<Issue> unknown = form.unknownFields(VERSION_FIELDS).stream()
                    .filter(i -> !"version".equals(i.name()))
                    .toList();
            checks.addAll(unknown).addAll(form.repeatedFields(VERSION_FIELDS));
            String description = checks.requiredText("description", form.text("description"));
            String changelog = checks.requiredText("changelog", form.text("changelog"));
            byte[] content = requireFile(form, checks);
            checks.throwIfAny();
            VocabularyService.Created created = service.addVersion(
                    new VocabularyService.Submission(vocabularyName, description, changelog, content, user.creator()));
            return createdResponse(created);
        }
    }

    @GetMapping(value = "/v1/vocabularies/{vocabularyName}/versions/{version}", produces = JSON)
    public Map<String, Object> getVocabularyVersion(
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        RequestChecks checks = RequestChecks.start().pathName("vocabularyName", vocabularyName);
        int number = checks.pathVersion(version);
        checks.noQuery(request.getQueryString()).throwIfAny();
        return representations.version(service.version(vocabularyName, number));
    }

    @PatchMapping(value = "/v1/vocabularies/{vocabularyName}/versions/{version}", produces = JSON, consumes = JSON)
    public Map<String, Object> updateVocabularyVersionStatus(
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        RequestChecks checks = RequestChecks.start().pathName("vocabularyName", vocabularyName);
        int number = checks.pathVersion(version);
        checks.noQuery(request.getQueryString());
        JsonNode body = readBodyAfter(checks, request);
        VocabularyStatus target = checks.status(body);
        checks.throwIfAny();
        return representations.version(service.setVersionStatus(vocabularyName, number, target));
    }

    private ResponseEntity<Map<String, Object>> createdResponse(VocabularyService.Created created) {
        Map<String, Object> body = representations.created(created.version(), created.warnings());
        return ResponseEntity.created(URI.create((String) body.get("href")))
                .header("Content-Type", JSON)
                .body(body);
    }

    /** Reads the JSON body; a body problem is added to the path and query issues of the request. */
    private JsonNode readBodyAfter(RequestChecks checks, HttpServletRequest request) {
        try {
            return jsonBody.readObject(request);
        } catch (ApiException ex) {
            if (ex.problemType() != eu.simpl.vocabularymanager.error.ProblemType.BAD_REQUEST) {
                throw ex;
            }
            checks.addAll(ex.issues()).throwIfAny();
            throw ex;
        }
    }

    static byte[] requireFile(MultipartForm form, RequestChecks checks) {
        byte[] content = form.bytes(MultipartForm.ContentField.FILE);
        if (content == null) {
            checks.add(Issue.schema("body", MultipartForm.ContentField.FILE, "Field 'vocabularyFile' is required."));
        }
        return content;
    }

    static long paging(QueryParameters query, String name, long max, long fallback, RequestChecks checks) {
        String raw = query.get(name);
        if (raw == null) {
            return fallback;
        }
        long value = Names.positiveInt(raw, max);
        if (value < 0) {
            checks.add(Issue.schema(
                    "query", name, "Query parameter '" + name + "' must be an integer from 1 to " + max + "."));
            return fallback;
        }
        return value;
    }

    private static ListQuery.TimeRange dateFilter(QueryParameters query, String name, RequestChecks checks) {
        String raw = query.get(name);
        if (raw == null) {
            return null;
        }
        return DateFilters.parse(raw).orElseGet(() -> {
            checks.add(
                    Issue.schema("query", name, "Query parameter '" + name + "' must be a valid date or date-time."));
            return null;
        });
    }

    /** One reference of a vocabulary search. */
    public record SearchReference(String name, Integer version) {}
}

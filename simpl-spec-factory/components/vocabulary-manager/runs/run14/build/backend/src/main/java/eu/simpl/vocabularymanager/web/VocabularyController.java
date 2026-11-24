package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.PageResponse;
import eu.simpl.vocabularymanager.api.SearchItemResponse;
import eu.simpl.vocabularymanager.api.VersionResponse;
import eu.simpl.vocabularymanager.api.VocabularyResponse;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Operations on internal vocabularies and their versions (OpenAPI tag "Vocabulary management"). */
@RestController
@RequestMapping("/v1")
public class VocabularyController {

    static final String JSON = "application/json";
    static final String PROBLEM = "application/problem+json";

    private static final Map<String, String> LIST_SORT = Map.of(
            "name", "ASC(?name)",
            "-name", "DESC(?name)",
            "status", "ASC(?status) ASC(?name)",
            "-status", "DESC(?status) ASC(?name)",
            "creationDate", "ASC(?created) ASC(?name)",
            "-creationDate", "DESC(?created) ASC(?name)",
            "lastUpdated", "ASC(?updated) ASC(?name)",
            "-lastUpdated", "DESC(?updated) ASC(?name)");
    private static final Map<String, String> VERSION_SORT = Map.of(
            "name", "DESC(?n)",
            "-name", "DESC(?n)",
            "status", "ASC(?vstatus) DESC(?n)",
            "-status", "DESC(?vstatus) DESC(?n)",
            "creationDate", "ASC(?vcreated) DESC(?n)",
            "-creationDate", "DESC(?vcreated) DESC(?n)");
    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    private static final int MAX_SEARCH_ITEMS = 100;

    private final VocabularyService service;
    private final ApiMapper mapper;

    public VocabularyController(VocabularyService service, ApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** GET /vocabularies (SIMPL-18162, SIMPL-18164, D19 to D23). */
    @GetMapping(
            path = "/vocabularies",
            produces = {JSON, PROBLEM})
    public ResponseEntity<PageResponse<VocabularyResponse>> getVocabularies(HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        query.requireOnly(Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize"));
        String name = RequestRules.nameFilter(query.get("name"));
        VocabularyStatus status = statusFilter(query.get("status"));
        Instant[] created = RequestRules.dateFilter("creationDate", query.get("creationDate"));
        Instant[] updated = RequestRules.dateFilter("lastUpdated", query.get("lastUpdated"));
        String orderBy = sort(query.get("sort"), LIST_SORT, "name");
        int page = RequestRules.queryInteger("page", query.get("page"), 1, RequestRules.MAX_INT);
        int pageSize = RequestRules.queryInteger(
                "pageSize", query.get("pageSize"), Paging.DEFAULT_PAGE_SIZE, Paging.MAX_PAGE_SIZE);
        VocabularyRepository.ListFilter filter = new VocabularyRepository.ListFilter(
                name == null ? null : name.toLowerCase(java.util.Locale.ROOT),
                status,
                created == null ? null : created[0],
                created == null ? null : created[1],
                updated == null ? null : updated[0],
                updated == null ? null : updated[1]);
        VocabularyService.Slice<Vocabulary> slice =
                service.list(filter, orderBy, (long) (page - 1) * pageSize, pageSize);
        List<VocabularyResponse> items =
                slice.items().stream().map(mapper::vocabulary).toList();
        return json(Paging.page("/vocabularies", query, page, pageSize, slice.total(), items));
    }

    /** POST /vocabularies (SIMPL-18170, SIMPL-18187 AC1, D7 to D15, D37, D51). */
    @PostMapping(
            path = "/vocabularies",
            consumes = "multipart/form-data",
            produces = {JSON, PROBLEM})
    public ResponseEntity<VersionResponse> createVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        form.checkShape(CREATE_FIELDS, Set.of());
        List<Issue> issues = new ArrayList<>();
        String name = RequestRules.nameField(form.text("name"), issues);
        String description = RequestRules.requiredText("description", form.text("description"), issues);
        String changelog = RequestRules.requiredText("changelog", form.text("changelog"), issues);
        byte[] file = requiredFile(form, issues);
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        VocabularyService.Created created = service.create(name, description, changelog, file, user(request));
        return createdResponse(created);
    }

    /** POST /vocabularies/searches (D38, D68, D110, D182, D205). */
    @PostMapping(
            path = "/vocabularies/searches",
            consumes = JSON,
            produces = {JSON, PROBLEM})
    public ResponseEntity<PageResponse<SearchItemResponse>> searchVocabularies(HttpServletRequest request) {
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        JsonNode body = JsonBody.readObject(request);
        JsonBody.requireOnly(body, "", Set.of("items"));
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > MAX_SEARCH_ITEMS) {
            throw ApiException.badRequest("body", "items", "Property 'items' must be an array of 1 to 100 items.");
        }
        List<VocabularyService.Reference> references = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            references.add(searchReference(items.get(i), "items[" + i + "]"));
        }
        List<SearchItemResponse> results =
                service.search(references).stream().map(mapper::searchItem).toList();
        return json(Paging.searchPage(results));
    }

    /** GET /vocabularies/{vocabularyName} (D25). */
    @GetMapping(
            path = "/vocabularies/{vocabularyName}",
            produces = {JSON, PROBLEM})
    public ResponseEntity<VocabularyResponse> getVocabulary(
            @PathVariable("vocabularyName") String rawName, HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        return json(mapper.vocabulary(service.get(name)));
    }

    /** PATCH /vocabularies/{vocabularyName} (SIMPL-18175, D3, D4). */
    @PatchMapping(
            path = "/vocabularies/{vocabularyName}",
            consumes = JSON,
            produces = {JSON, PROBLEM})
    public ResponseEntity<VocabularyResponse> updateVocabularyStatus(
            @PathVariable("vocabularyName") String rawName, HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        VocabularyStatus target = statusBody(request);
        return json(mapper.vocabulary(service.changeStatus(name, target, user(request))));
    }

    /** GET /vocabularies/{vocabularyName}/versions (SIMPL-18168, D23, D154, D196). */
    @GetMapping(
            path = "/vocabularies/{vocabularyName}/versions",
            produces = {JSON, PROBLEM})
    public ResponseEntity<PageResponse<VersionResponse>> getVocabularyVersions(
            @PathVariable("vocabularyName") String rawName, HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        query.requireOnly(Set.of("sort", "page", "pageSize"));
        String orderBy = sort(query.get("sort"), VERSION_SORT, null);
        int page = RequestRules.queryInteger("page", query.get("page"), 1, RequestRules.MAX_INT);
        int pageSize = RequestRules.queryInteger(
                "pageSize", query.get("pageSize"), Paging.DEFAULT_PAGE_SIZE, Paging.MAX_PAGE_SIZE);
        VocabularyService.Slice<VocabularyVersion> slice =
                service.versions(name, orderBy == null ? "DESC(?n)" : orderBy, (long) (page - 1) * pageSize, pageSize);
        List<VersionResponse> items = slice.items().stream()
                .map(version -> mapper.version(version, slice.total(), null))
                .toList();
        return json(Paging.page("/vocabularies/" + name + "/versions", query, page, pageSize, slice.total(), items));
    }

    /** POST /vocabularies/{vocabularyName}/versions (SIMPL-18173, SIMPL-18190, D4, D7, D43, D46). */
    @PostMapping(
            path = "/vocabularies/{vocabularyName}/versions",
            consumes = "multipart/form-data",
            produces = {JSON, PROBLEM})
    public ResponseEntity<VersionResponse> createVocabularyVersion(
            @PathVariable("vocabularyName") String rawName, HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request);
        String name = RequestRules.vocabularyName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        form.checkShape(VERSION_FIELDS, Set.of());
        List<Issue> issues = new ArrayList<>();
        String description = RequestRules.requiredText("description", form.text("description"), issues);
        String changelog = RequestRules.requiredText("changelog", form.text("changelog"), issues);
        byte[] file = requiredFile(form, issues);
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        VocabularyService.Created created = service.createVersion(name, description, changelog, file, user(request));
        return createdResponse(created);
    }

    /** GET /vocabularies/{vocabularyName}/versions/{version} (SIMPL-18166). */
    @GetMapping(
            path = "/vocabularies/{vocabularyName}/versions/{version}",
            produces = {JSON, PROBLEM})
    public ResponseEntity<VersionResponse> getVocabularyVersion(
            @PathVariable("vocabularyName") String rawName,
            @PathVariable("version") String rawVersion,
            HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        long version = RequestRules.version(rawVersion);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        VocabularyService.Created found = service.version(name, version);
        return json(mapper.version(found.version(), found.latestVersion(), null));
    }

    /** PATCH /vocabularies/{vocabularyName}/versions/{version} (D4, D41, D160). */
    @PatchMapping(
            path = "/vocabularies/{vocabularyName}/versions/{version}",
            consumes = JSON,
            produces = {JSON, PROBLEM})
    public ResponseEntity<VersionResponse> updateVocabularyVersionStatus(
            @PathVariable("vocabularyName") String rawName,
            @PathVariable("version") String rawVersion,
            HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        long version = RequestRules.version(rawVersion);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        VocabularyStatus target = statusBody(request);
        VocabularyService.Created changed = service.changeVersionStatus(name, version, target, user(request));
        return json(mapper.version(changed.version(), changed.latestVersion(), null));
    }

    private ResponseEntity<VersionResponse> createdResponse(VocabularyService.Created created) {
        VersionResponse body = mapper.version(created.version(), created.latestVersion(), created.warnings());
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static VocabularyService.Reference searchReference(JsonNode item, String prefix) {
        if (item == null || !item.isObject()) {
            throw ApiException.badRequest("body", prefix, "Item '" + prefix + "' must be an object.");
        }
        JsonBody.requireOnly(item, prefix + ".", Set.of("name", "version"));
        JsonNode name = item.get("name");
        if (name == null
                || !name.isString()
                || !eu.simpl.vocabularymanager.domain.VocabularyNames.isValid(name.asString())) {
            throw ApiException.badRequest(
                    "body", prefix + ".name", "Property '" + prefix + ".name' must be a valid vocabulary name.");
        }
        JsonNode version = item.get("version");
        if (version == null) {
            return new VocabularyService.Reference(name.asString(), null);
        }
        if (!JsonBody.isInteger(version, 1)) {
            throw ApiException.badRequest(
                    "body",
                    prefix + ".version",
                    "Property '" + prefix + ".version' must be an integer from 1 to 2147483647.");
        }
        return new VocabularyService.Reference(name.asString(), version.asLong());
    }

    private static VocabularyStatus statusBody(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        JsonBody.requireOnly(body, "", Set.of("status"));
        JsonNode status = body.get("status");
        VocabularyStatus target =
                status != null && status.isString() ? VocabularyStatus.parse(status.asString()) : null;
        if (target == null) {
            throw ApiException.badRequest("body", "status", "Property 'status' must be ACTIVE or DEPRECATED.");
        }
        return target;
    }

    private static VocabularyStatus statusFilter(String value) {
        if (value == null) {
            return null;
        }
        VocabularyStatus status = VocabularyStatus.parse(value);
        if (status == null) {
            throw ApiException.badRequest("query", "status", "Query parameter 'status' must be ACTIVE or DEPRECATED.");
        }
        return status;
    }

    private static String sort(String value, Map<String, String> allowed, String fallback) {
        if (value == null) {
            return fallback == null ? null : allowed.get(fallback);
        }
        String orderBy = allowed.get(value);
        if (orderBy == null) {
            throw ApiException.badRequest(
                    "query", "sort", "Query parameter 'sort' must be one of " + allowed.keySet() + ".");
        }
        return orderBy;
    }

    private static byte[] requiredFile(MultipartForm form, List<Issue> issues) {
        byte[] file = form.bytes(MultipartForm.FILE_FIELD);
        if (file == null) {
            issues.add(Issue.schemaViolation("body", MultipartForm.FILE_FIELD, "Field 'vocabularyFile' is required."));
        }
        return file;
    }

    static AuthenticatedUser user(HttpServletRequest request) {
        return (AuthenticatedUser) request.getAttribute(GatekeeperFilter.USER);
    }

    static <T> ResponseEntity<T> json(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}

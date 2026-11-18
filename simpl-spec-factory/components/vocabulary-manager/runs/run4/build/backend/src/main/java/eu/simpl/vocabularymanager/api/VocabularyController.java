package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.vocabularymanager.api.ApiModel.CollectionPage;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyCategory;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyMetadata;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularySearchItem;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyVersionMetadataResponse;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.service.Caller;
import eu.simpl.vocabularymanager.service.DateFilter;
import eu.simpl.vocabularymanager.service.PageRequest;
import eu.simpl.vocabularymanager.service.SortOrder;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVersion;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVocabulary;
import eu.simpl.vocabularymanager.service.VocabularyCommands.Created;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchReference;
import eu.simpl.vocabularymanager.service.VocabularyCommands.VersionPage;
import eu.simpl.vocabularymanager.service.VocabularyCommands.VersionView;
import eu.simpl.vocabularymanager.service.VocabularyFilter;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Vocabulary management operations of the OpenAPI (tag 'Vocabulary management'). */
@RestController
public class VocabularyController {

    static final String FILE = "vocabularyFile";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String MULTIPART = MediaType.MULTIPART_FORM_DATA_VALUE;
    private static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMETERS = Set.of("sort", "page", "pageSize");
    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    private static final String VOCABULARY_SORTS =
            "[name, -name, status, -status, creationDate, -creationDate, lastUpdated, -lastUpdated]";
    private static final String VERSION_SORTS = "[name, -name, status, -status, creationDate, -creationDate]";

    private final VocabularyService service;
    private final ApiMapper mapper;
    private final JsonBodies jsonBodies;
    private final long maxUploadBytes;

    public VocabularyController(
            VocabularyService service,
            ApiMapper mapper,
            JsonBodies jsonBodies,
            VocabularyManagerProperties properties) {
        this.service = service;
        this.mapper = mapper;
        this.jsonBodies = jsonBodies;
        this.maxUploadBytes = properties.maxUploadBytes();
    }

    /** GET /vocabularies (SIMPL-18162, SIMPL-18164). */
    @GetMapping(value = "/vocabularies", produces = JSON)
    public CollectionPage<VocabularyCategory> list(HttpServletRequest request) {
        QueryParameters query = QueryParameters.read(request, LIST_PARAMETERS);
        List<Issue> issues = new ArrayList<>();
        String name = query.get("name");
        if (name != null && !NAME_FILTER.matcher(name).matches()) {
            issues.add(Issue.schemaViolation(
                    "query", "name", name, "Query parameter 'name' must have 1 to 64 letters or digits."));
        }
        VocabularyStatus status = null;
        if (query.get("status") != null) {
            status = VocabularyStatus.fromWire(query.get("status")).orElse(null);
            if (status == null) {
                issues.add(Issue.schemaViolation(
                        "query",
                        "status",
                        query.get("status"),
                        "Query parameter 'status' must be one of [ACTIVE, DEPRECATED]."));
            }
        }
        DateFilter creationDate = dateFilter(query, "creationDate", issues);
        DateFilter lastUpdated = dateFilter(query, "lastUpdated", issues);
        Optional<Comparator<Vocabulary>> order = SortOrder.vocabularies(query.get("sort"));
        if (order.isEmpty()) {
            issues.add(sortIssue(query.get("sort"), VOCABULARY_SORTS));
        }
        PageRequest pageRequest = query.pageRequest(issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        Map<String, String> links = linkParameters(query, "sort", "name", "status", "creationDate", "lastUpdated");
        return mapper.page(
                service.list(new VocabularyFilter(name, status, creationDate, lastUpdated), order.get(), pageRequest),
                "/vocabularies",
                links,
                mapper::vocabulary);
    }

    /** POST /vocabularies (SIMPL-18170, SIMPL-18187). */
    @PostMapping(value = "/vocabularies", consumes = MULTIPART, produces = JSON)
    public ResponseEntity<VocabularyVersionMetadataResponse> create(
            HttpServletRequest request, Authentication authentication) throws IOException, ServletException {
        QueryParameters.read(request, Set.of());
        MultipartForm form =
                MultipartForm.read(request, FILE, Set.of(FILE, "name", "description", "changelog"), Map.of());
        List<Issue> issues = new ArrayList<>();
        String name = FormFields.name(form, issues);
        String description = FormFields.requiredText(form, "description", issues);
        String changelog = FormFields.requiredText(form, "changelog", issues);
        byte[] content = FormFields.file(form, FILE, issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        checkSize(content);
        Created created = service.create(
                new CreateVocabulary(name, description, changelog, content, new Caller(authentication.getName())));
        return createdResponse(created);
    }

    /** POST /vocabularies/searches (decision D38). */
    @PostMapping(value = "/vocabularies/searches", consumes = JSON, produces = JSON)
    public CollectionPage<VocabularySearchItem> search(
            HttpServletRequest request, @RequestBody(required = false) String body) {
        QueryParameters.read(request, Set.of());
        JsonNode node = jsonBodies.object(body, Set.of("items"));
        JsonNode items = node.get("items");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > 100) {
            throw Problems.badRequest(Issue.schemaViolation(
                    "body", "items", null, "Property 'items' must be an array of 1 to 100 references."));
        }
        List<SearchReference> references = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            references.add(searchReference(items.get(i), "items[" + i + "]", issues));
        }
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return mapper.searchResult(
                service.search(references).stream().map(mapper::searchItem).toList());
    }

    /** GET /vocabularies/{vocabularyName}. */
    @GetMapping(value = "/vocabularies/{vocabularyName}", produces = JSON)
    public VocabularyCategory get(@PathVariable String vocabularyName, HttpServletRequest request) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        QueryParameters.read(request, Set.of());
        return mapper.vocabulary(service.get(name));
    }

    /** PATCH /vocabularies/{vocabularyName} (SIMPL-18175, decision D4). */
    @PatchMapping(value = "/vocabularies/{vocabularyName}", consumes = JSON, produces = JSON)
    public VocabularyCategory updateStatus(
            @PathVariable String vocabularyName,
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        QueryParameters.read(request, Set.of());
        VocabularyStatus status = statusOf(body);
        return mapper.vocabulary(service.updateStatus(name, status));
    }

    /** GET /vocabularies/{vocabularyName}/versions (SIMPL-18168). */
    @GetMapping(value = "/vocabularies/{vocabularyName}/versions", produces = JSON)
    public CollectionPage<VocabularyMetadata> versions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        QueryParameters query = QueryParameters.read(request, VERSION_LIST_PARAMETERS);
        List<Issue> issues = new ArrayList<>();
        Optional<Comparator<VocabularyVersion>> order = SortOrder.versions(query.get("sort"));
        if (order.isEmpty()) {
            issues.add(sortIssue(query.get("sort"), VERSION_SORTS));
        }
        PageRequest pageRequest = query.pageRequest(issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        VersionPage page = service.listVersions(name, order.get(), pageRequest);
        return mapper.page(
                page.page(),
                ApiMapper.vocabularyHref(name) + "/versions",
                linkParameters(query, "sort"),
                v -> mapper.version(v, page.latestVersion()));
    }

    /** POST /vocabularies/{vocabularyName}/versions (SIMPL-18173, SIMPL-18190, decisions D7, D46). */
    @PostMapping(value = "/vocabularies/{vocabularyName}/versions", consumes = MULTIPART, produces = JSON)
    public ResponseEntity<VocabularyVersionMetadataResponse> createVersion(
            @PathVariable String vocabularyName, HttpServletRequest request, Authentication authentication)
            throws IOException, ServletException {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        QueryParameters.read(request, Set.of());
        MultipartForm form = MultipartForm.read(
                request,
                FILE,
                Set.of(FILE, "description", "changelog"),
                Map.of("version", "Field 'version' must not be provided; version numbers are assigned automatically."));
        List<Issue> issues = new ArrayList<>();
        String description = FormFields.requiredText(form, "description", issues);
        String changelog = FormFields.requiredText(form, "changelog", issues);
        byte[] content = FormFields.file(form, FILE, issues);
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        checkSize(content);
        Created created = service.createVersion(
                name, new CreateVersion(description, changelog, content, new Caller(authentication.getName())));
        return createdResponse(created);
    }

    /** GET /vocabularies/{vocabularyName}/versions/{version} (SIMPL-18166). */
    @GetMapping(value = "/vocabularies/{vocabularyName}/versions/{version}", produces = JSON)
    public VocabularyMetadata version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        int number = PathParameters.version(version);
        QueryParameters.read(request, Set.of());
        VersionView view = service.getVersion(name, number);
        return mapper.version(view.version(), view.latestVersion());
    }

    /** PATCH /vocabularies/{vocabularyName}/versions/{version} (decision D4). */
    @PatchMapping(value = "/vocabularies/{vocabularyName}/versions/{version}", consumes = JSON, produces = JSON)
    public VocabularyMetadata updateVersionStatus(
            @PathVariable String vocabularyName,
            @PathVariable String version,
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        int number = PathParameters.version(version);
        QueryParameters.read(request, Set.of());
        VocabularyStatus status = statusOf(body);
        VersionView view = service.updateVersionStatus(name, number, status);
        return mapper.version(view.version(), view.latestVersion());
    }

    private ResponseEntity<VocabularyVersionMetadataResponse> createdResponse(Created created) {
        VocabularyVersion version = created.version();
        return ResponseEntity.created(URI.create(ApiMapper.versionHref(version.name(), version.version())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.created(version, created.warnings()));
    }

    private void checkSize(byte[] content) {
        if (content.length > maxUploadBytes) {
            throw Problems.uploadTooLarge();
        }
    }

    private VocabularyStatus statusOf(String body) {
        JsonNode node = jsonBodies.object(body, Set.of("status"));
        JsonNode status = node.get("status");
        if (status == null || status.isNull()) {
            throw Problems.badRequest(Issue.schemaViolation("body", "status", null, "Property 'status' is required."));
        }
        return Optional.of(status)
                .filter(JsonNode::isTextual)
                .flatMap(s -> VocabularyStatus.fromWire(s.asText()))
                .orElseThrow(() -> Problems.badRequest(Issue.schemaViolation(
                        "body",
                        "status",
                        status.isTextual() ? status.asText() : status.toString(),
                        "Property 'status' must be one of [ACTIVE, DEPRECATED].")));
    }

    private static SearchReference searchReference(JsonNode item, String path, List<Issue> issues) {
        if (item == null || !item.isObject()) {
            issues.add(Issue.schemaViolation("body", path, null, "Each reference must be a JSON object."));
            return null;
        }
        item.fieldNames().forEachRemaining(field -> {
            if (!field.equals("name") && !field.equals("version")) {
                issues.add(Issue.schemaViolation(
                        "body", path + "." + field, null, "Property '" + field + "' is not supported."));
            }
        });
        JsonNode name = item.get("name");
        if (name == null || !name.isTextual() || !PathParameters.isValidName(name.asText())) {
            issues.add(Issue.schemaViolation(
                    "body",
                    path + ".name",
                    name == null ? null : name.asText(),
                    "Property 'name' is required and must have 3 to 64 characters matching ^[A-Z][a-zA-Z0-9]*$."));
            return null;
        }
        JsonNode version = item.get("version");
        if (version == null || version.isNull()) {
            return new SearchReference(name.asText(), null);
        }
        if (!version.isIntegralNumber() || !version.canConvertToInt() || version.asInt() < 1) {
            issues.add(Issue.schemaViolation(
                    "body",
                    path + ".version",
                    version.toString(),
                    "Property 'version' must be an integer of at least 1."));
            return null;
        }
        return new SearchReference(name.asText(), version.asInt());
    }

    private static DateFilter dateFilter(QueryParameters query, String parameter, List<Issue> issues) {
        String value = query.get(parameter);
        if (value == null) {
            return null;
        }
        Optional<DateFilter> filter = DATE_FILTER.matcher(value).matches() ? DateFilter.parse(value) : Optional.empty();
        if (filter.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    "query",
                    parameter,
                    value,
                    "Query parameter '" + parameter + "' must be a valid ISO 8601 date (YYYY-MM-DD) or date-time."));
            return null;
        }
        return filter.get();
    }

    private static Issue sortIssue(String value, String allowed) {
        return Issue.schemaViolation("query", "sort", value, "Query parameter 'sort' must be one of " + allowed + ".");
    }

    private static Map<String, String> linkParameters(QueryParameters query, String... names) {
        Map<String, String> links = new LinkedHashMap<>();
        for (String name : names) {
            if (query.get(name) != null) {
                links.put(name, query.get(name));
            }
        }
        return links;
    }
}

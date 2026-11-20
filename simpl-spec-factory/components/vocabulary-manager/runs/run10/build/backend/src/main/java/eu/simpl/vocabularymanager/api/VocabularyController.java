package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.Dtos;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.request.FieldRules;
import eu.simpl.vocabularymanager.request.JsonBody;
import eu.simpl.vocabularymanager.request.MultipartForm;
import eu.simpl.vocabularymanager.request.Paging;
import eu.simpl.vocabularymanager.request.QueryParameters;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.SearchService;
import eu.simpl.vocabularymanager.service.UploadLimiter;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.ListQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vocabulary and vocabulary version operations (tag "Vocabulary management"). */
@RestController
public class VocabularyController {

    static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    static final Set<String> LIST_SORTS = Set.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    static final Set<String> VERSION_SORTS =
            Set.of("name", "-name", "status", "-status", "creationDate", "-creationDate");
    private static final Pattern DATE_FILTER = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");
    static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    static final int SEARCH_MAX = 100;

    private final VocabularyService vocabularies;
    private final SearchService search;
    private final UploadLimiter uploads;
    private final Representations representations;

    public VocabularyController(
            VocabularyService vocabularies,
            SearchService search,
            UploadLimiter uploads,
            Representations representations) {
        this.vocabularies = vocabularies;
        this.search = search;
        this.uploads = uploads;
        this.representations = representations;
    }

    @GetMapping("/v1/vocabularies")
    public ResponseEntity<Dtos.CollectionPage<Dtos.VocabularyCategory>> getVocabularies(HttpServletRequest request) {
        Checks checks = new Checks();
        QueryParameters parameters = checks.query(request, LIST_PARAMETERS);
        String name = parameters.get("name");
        if (name != null
                && (name.length() > FieldRules.NAME_MAX
                        || !FieldRules.NAME_FILTER.matcher(name).matches())) {
            checks.issues.add(
                    Issue.schemaViolation("query", "name", "'name' must be 1 to 64 letters or digits.", name));
        }
        VocabularyStatus status = null;
        if (parameters.has("status")) {
            status = VocabularyStatus.parse(parameters.get("status"));
            if (status == null) {
                checks.issues.add(Issue.schemaViolation(
                        "query", "status", "'status' must be one of [ACTIVE, DEPRECATED].", parameters.get("status")));
            }
        }
        Instant[] created = dateRange(parameters, "creationDate", checks.issues);
        Instant[] updated = dateRange(parameters, "lastUpdated", checks.issues);
        String sort = parameters.has("sort") ? parameters.get("sort") : "name";
        if (!LIST_SORTS.contains(sort)) {
            checks.issues.add(
                    Issue.schemaViolation("query", "sort", "'sort' must be one of " + LIST_SORTS + ".", sort));
        }
        Paging paging = Paging.from(parameters, checks.issues);
        checks.throwIfAny();
        boolean descending = sort.startsWith("-");
        ListQuery query = new ListQuery(
                name,
                status,
                created == null ? null : created[0],
                created == null ? null : created[1],
                updated == null ? null : updated[0],
                updated == null ? null : updated[1],
                descending ? sort.substring(1) : sort,
                descending,
                paging.offset(),
                paging.pageSize());
        VocabularyService.Page<Vocabulary> page = vocabularies.list(query);
        List<Dtos.VocabularyCategory> items =
                page.items().stream().map(representations::vocabulary).toList();
        return json(Representations.page(items, page.total(), paging, "/vocabularies", parameters));
    }

    @PostMapping("/v1/vocabularies")
    public ResponseEntity<Dtos.VocabularyVersionMetadataResponse> createVocabulary(HttpServletRequest request) {
        return uploads.run(() -> {
            MultipartForm form = MultipartForm.read(request, "vocabularyFile");
            Checks checks = new Checks();
            checks.noQuery(request);
            form.checkNames(CREATE_FIELDS, Set.of(), checks.issues);
            String name = FieldRules.name(form.text("name"), "body", "name", checks.issues);
            VocabularyService.Upload upload = upload(form, checks);
            checks.throwIfAny();
            VocabularyService.Created created = vocabularies.create(name, upload);
            return createdResponse(created);
        });
    }

    @PostMapping("/v1/vocabularies/searches")
    public ResponseEntity<Dtos.CollectionPage<Dtos.VocabularySearchItem>> searchVocabularies(
            HttpServletRequest request) {
        Map<String, Object> body = JsonBody.read(request);
        Checks checks = new Checks();
        checks.noQuery(request);
        List<SearchService.Reference> references = searchReferences(body, checks.issues);
        checks.throwIfAny();
        List<Dtos.VocabularySearchItem> items = search.searchInternal(references).stream()
                .map(representations::searchItem)
                .toList();
        return json(Representations.searchPage(items));
    }

    @GetMapping("/v1/vocabularies/{vocabularyName}")
    public ResponseEntity<Dtos.VocabularyCategory> getVocabulary(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        checks.throwIfAny();
        return json(representations.vocabulary(vocabularies.get(name)));
    }

    @PatchMapping("/v1/vocabularies/{vocabularyName}")
    public ResponseEntity<Dtos.VocabularyCategory> updateVocabularyStatus(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        Map<String, Object> body = JsonBody.read(request);
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        VocabularyStatus target = statusOf(body, checks.issues);
        checks.throwIfAny();
        return json(representations.vocabulary(vocabularies.changeStatus(name, target)));
    }

    @GetMapping("/v1/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<Dtos.CollectionPage<Dtos.VocabularyMetadata>> getVocabularyVersions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        Checks checks = new Checks();
        QueryParameters parameters = checks.query(request, Set.of("sort", "page", "pageSize"));
        String name = checks.pathName(vocabularyName, "vocabularyName");
        String sort = parameters.get("sort");
        if (sort != null && !VERSION_SORTS.contains(sort)) {
            checks.issues.add(
                    Issue.schemaViolation("query", "sort", "'sort' must be one of " + VERSION_SORTS + ".", sort));
        }
        Paging paging = Paging.from(parameters, checks.issues);
        checks.throwIfAny();
        VocabularyService.Page<eu.simpl.vocabularymanager.domain.VocabularyVersion> page =
                vocabularies.versions(name, sort, paging.offset(), paging.pageSize());
        int latest = (int) page.total();
        List<Dtos.VocabularyMetadata> items = page.items().stream()
                .map(version -> representations.version(version, latest))
                .toList();
        return json(Representations.page(
                items, page.total(), paging, Representations.vocabularyHref(name) + "/versions", parameters));
    }

    @PostMapping("/v1/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<Dtos.VocabularyVersionMetadataResponse> createVocabularyVersion(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        return uploads.run(() -> {
            MultipartForm form = MultipartForm.read(request, "vocabularyFile");
            Checks checks = new Checks();
            checks.noQuery(request);
            String name = checks.pathName(vocabularyName, "vocabularyName");
            if (form.has("version")) {
                checks.issues.add(Issue.schemaViolation(
                        "body", "version", "Version numbers are assigned automatically; do not send 'version'.", null));
            }
            List<Issue> nameIssues = new ArrayList<>();
            form.checkNames(VERSION_FIELDS, Set.of(), nameIssues);
            nameIssues.stream().filter(issue -> !"version".equals(issue.name())).forEach(checks.issues::add);
            VocabularyService.Upload upload = upload(form, checks);
            checks.throwIfAny();
            return createdResponse(vocabularies.createVersion(name, upload));
        });
    }

    @GetMapping("/v1/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<Dtos.VocabularyMetadata> getVocabularyVersion(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        Integer number = checks.pathVersion(version);
        checks.throwIfAny();
        VocabularyService.VersionView view = vocabularies.getVersion(name, number);
        return json(representations.version(view.version(), view.latestVersion()));
    }

    @PatchMapping("/v1/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<Dtos.VocabularyMetadata> updateVocabularyVersionStatus(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        Map<String, Object> body = JsonBody.read(request);
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        Integer number = checks.pathVersion(version);
        VocabularyStatus target = statusOf(body, checks.issues);
        checks.throwIfAny();
        VocabularyService.VersionView view = vocabularies.changeVersionStatus(name, number, target);
        return json(representations.version(view.version(), view.latestVersion()));
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private VocabularyService.Upload upload(MultipartForm form, Checks checks) {
        String description = FieldRules.text(form.text("description"), "description", true, checks.issues);
        String changelog = FieldRules.text(form.text("changelog"), "changelog", true, checks.issues);
        byte[] content = form.bytes("vocabularyFile");
        if (content == null) {
            checks.issues.add(Issue.schemaViolation("body", "vocabularyFile", "'vocabularyFile' is required.", null));
        }
        return new VocabularyService.Upload(
                description, changelog, content, currentUser().creator());
    }

    private ResponseEntity<Dtos.VocabularyVersionMetadataResponse> createdResponse(VocabularyService.Created created) {
        Dtos.VocabularyVersionMetadataResponse body = representations.created(created.version(), created.warnings());
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    static VocabularyStatus statusOf(Map<String, Object> body, List<Issue> issues) {
        for (String key : body.keySet()) {
            if (!"status".equals(key)) {
                issues.add(Issue.schemaViolation("body", key, "Property '" + key + "' is not supported.", null));
            }
        }
        if (!body.containsKey("status")) {
            issues.add(Issue.schemaViolation("body", "status", "'status' is required.", null));
            return null;
        }
        VocabularyStatus status = VocabularyStatus.parse(body.get("status"));
        if (status == null) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "status",
                    "'status' must be one of [ACTIVE, DEPRECATED].",
                    String.valueOf(body.get("status"))));
        }
        return status;
    }

    static List<SearchService.Reference> searchReferences(Map<String, Object> body, List<Issue> issues) {
        for (String key : body.keySet()) {
            if (!"items".equals(key)) {
                issues.add(Issue.schemaViolation("body", key, "Property '" + key + "' is not supported.", null));
            }
        }
        Object items = body.get("items");
        if (!(items instanceof List<?> list) || list.isEmpty() || list.size() > SEARCH_MAX) {
            issues.add(Issue.schemaViolation(
                    "body", "items", "'items' must be an array of 1 to " + SEARCH_MAX + " references.", null));
            return List.of();
        }
        List<SearchService.Reference> references = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String prefix = "items[" + i + "]";
            if (!(list.get(i) instanceof Map<?, ?> item)) {
                issues.add(Issue.schemaViolation("body", prefix, "Each item must be an object.", null));
                continue;
            }
            for (Object key : item.keySet()) {
                if (!"name".equals(key) && !"version".equals(key)) {
                    issues.add(Issue.schemaViolation(
                            "body", prefix + "." + key, "Property '" + key + "' is not supported.", null));
                }
            }
            Object nameValue = item.get("name");
            String name = nameValue instanceof String text && FieldRules.isValidName(text) ? text : null;
            if (name == null) {
                issues.add(Issue.schemaViolation(
                        "body", prefix + ".name", "'name' is required and must be a valid vocabulary name.", null));
            }
            Integer version = null;
            if (item.containsKey("version")) {
                Object value = item.get("version");
                if (JsonBody.isInteger(value)
                        && new BigInteger(value.toString()).compareTo(BigInteger.ONE) >= 0
                        && new BigInteger(value.toString()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
                    version = Integer.valueOf(value.toString());
                } else {
                    issues.add(Issue.schemaViolation(
                            "body",
                            prefix + ".version",
                            "'version' must be a whole number from 1 to 2147483647.",
                            null));
                    continue;
                }
            }
            if (name != null) {
                references.add(new SearchService.Reference(name, version));
            }
        }
        return references;
    }

    /** Date filter of D20: a date is a UTC day, a date-time an exact second (no offset means UTC, D110). */
    static Instant[] dateRange(QueryParameters parameters, String field, List<Issue> issues) {
        if (!parameters.has(field)) {
            return null;
        }
        String value = parameters.get(field);
        if (!DATE_FILTER.matcher(value).matches()) {
            issues.add(Issue.schemaViolation(
                    "query", field, "'" + field + "' must be a date (YYYY-MM-DD) or a date-time.", value));
            return null;
        }
        try {
            if (value.length() == 10) {
                Instant start =
                        LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
                return new Instant[] {start, start.plus(1, ChronoUnit.DAYS)};
            }
            Instant moment;
            try {
                moment = OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException e) {
                moment = LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            }
            Instant start = moment.truncatedTo(ChronoUnit.SECONDS);
            return new Instant[] {start, start.plusSeconds(1)};
        } catch (DateTimeParseException e) {
            issues.add(
                    Issue.schemaViolation("query", field, "'" + field + "' is not a valid date or date-time.", value));
            return null;
        }
    }

    static AuthenticatedUser currentUser() {
        return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();
    }

    static <T> ResponseEntity<T> json(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}

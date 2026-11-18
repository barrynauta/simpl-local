package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyNames;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.CreationResult;
import eu.simpl.vocabularymanager.service.SearchReference;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Vocabulary and vocabulary version operations of the OpenAPI, with the decisions that extend it (D39). */
@RestController
@RequestMapping("/v1/vocabularies")
public class VocabularyController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VocabularyController.class);

    private static final Set<String> LIST_PARAMS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMS = Set.of("sort", "page", "pageSize");
    private static final List<String> LIST_SORTS = List.of(
            "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated");
    private static final List<String> VERSION_SORTS =
            List.of("name", "-name", "status", "-status", "creationDate", "-creationDate");
    private static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    private static final String FILE = "vocabularyFile";
    private static final int MAX_SEARCH_ITEMS = 100;

    private final VocabularyService service;
    private final RequestInspector inspector;
    private final Representations representations;
    private final long uploadLimit;

    public VocabularyController(
            VocabularyService service,
            RequestInspector inspector,
            Representations representations,
            VocabularyProperties properties) {
        this.service = service;
        this.inspector = inspector;
        this.representations = representations;
        this.uploadLimit = properties.uploadLimitBytes();
    }

    /** GET /vocabularies (SIMPL-18162, SIMPL-18164; D19 to D23). */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CollectionPage<Representations.VocabularyBody> list(HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        Map<String, List<String>> params = inspector.query(request);
        List<Issue> issues = inspector.queryIssues(params, LIST_PARAMS);
        String nameFilter = single(params, "name");
        if (nameFilter != null && !NAME_FILTER.matcher(nameFilter).matches()) {
            issues.add(Issue.schemaViolation(
                    "query", "name", "Query parameter 'name' must have 1 to 64 letters or digits.", nameFilter));
        }
        String statusValue = single(params, "status");
        Status status = statusValue == null ? null : Status.fromWire(statusValue);
        if (statusValue != null && status == null) {
            issues.add(Issue.schemaViolation(
                    "query", "status", "Query parameter 'status' must be one of [ACTIVE, DEPRECATED].", statusValue));
        }
        DateFilter created = dateFilter(params, "creationDate", issues);
        DateFilter updated = dateFilter(params, "lastUpdated", issues);
        String sort = sortValue(params, LIST_SORTS, issues);
        Paging paging = Paging.read(params, issues);
        PathValues.failOn(issues);

        String needle = nameFilter == null ? null : nameFilter.toLowerCase(Locale.ROOT);
        List<Vocabulary> matching = service.listVocabularies().stream()
                .filter(v -> needle == null || v.name().toLowerCase(Locale.ROOT).contains(needle))
                .filter(v -> status == null || v.status() == status)
                .filter(v -> created == null || created.matches(v.creationDate()))
                .filter(v -> updated == null || updated.matches(v.lastUpdateDate()))
                .sorted(vocabularyOrder(sort))
                .toList();
        List<Representations.VocabularyBody> bodies =
                matching.stream().map(representations::vocabulary).toList();
        String extra = linkQuery(params, List.of("name", "status", "creationDate", "lastUpdated", "sort"));
        return CollectionPage.of(bodies, paging.page(), paging.pageSize(), "/vocabularies", extra);
    }

    /** POST /vocabularies (SIMPL-18170, SIMPL-18187; D6 to D15, D37, D51). */
    @PostMapping
    public ResponseEntity<Representations.CreatedVersionBody> create(HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireMultipart(request);
        List<FormPart> parts = inspector.multipart(request, FILE, uploadLimit);
        List<Issue> issues = noQueryAllowed(request);
        FormFields fields = new FormFields(parts, CREATE_FIELDS, Set.of());
        byte[] content = fields.requiredFile(FILE);
        String name = fields.requiredName("name");
        String description = fields.requiredText("description");
        String changelog = fields.requiredText("changelog");
        issues.addAll(fields.issues());
        PathValues.failOn(issues);

        CreationResult result = service.createVocabulary(name, description, changelog, content, user.creator());
        LOG.info("Administrative change: vocabulary {} created by user {}", name, user.subject());
        return created(result);
    }

    /** POST /vocabularies/searches (D38). */
    @PostMapping("/searches")
    public CollectionPage<Representations.SearchItemBody> search(HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireJson(request);
        List<Issue> issues = noQueryAllowed(request);
        JsonNode body = inspector.jsonObject(request);
        List<SearchReference> references = searchReferences(body, issues);
        PathValues.failOn(issues);
        return CollectionPage.unpaged(service.search(references).stream()
                .map(representations::searchItem)
                .toList());
    }

    /** GET /vocabularies/{vocabularyName} (D25). */
    @GetMapping(path = "/{vocabularyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Representations.VocabularyBody get(@PathVariable String vocabularyName, HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = inspector.queryIssues(inspector.query(request), Set.of());
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        PathValues.failOn(issues);
        return representations.vocabulary(service.getVocabulary(vocabularyName));
    }

    /** PATCH /vocabularies/{vocabularyName} (SIMPL-18175; D3, D4). */
    @PatchMapping(path = "/{vocabularyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Representations.VocabularyBody updateStatus(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireJson(request);
        List<Issue> issues = noQueryAllowed(request);
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        Status status = statusFromBody(inspector.jsonObject(request), issues);
        PathValues.failOn(issues);
        return representations.vocabulary(service.updateVocabularyStatus(vocabularyName, status, user.subject()));
    }

    /** GET /vocabularies/{vocabularyName}/versions (SIMPL-18168; D22, D23). */
    @GetMapping(path = "/{vocabularyName}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public CollectionPage<Representations.VersionBody> versions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        Map<String, List<String>> params = inspector.query(request);
        List<Issue> issues = inspector.queryIssues(params, VERSION_LIST_PARAMS);
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        String sort = sortValue(params, VERSION_SORTS, issues);
        Paging paging = Paging.read(params, issues);
        PathValues.failOn(issues);

        List<VocabularyVersion> versions = new ArrayList<>(service.listVersions(vocabularyName));
        int latest =
                versions.stream().mapToInt(VocabularyVersion::version).max().orElse(1);
        versions.sort(versionOrder(sort));
        List<Representations.VersionBody> bodies =
                versions.stream().map(v -> representations.version(v, latest)).toList();
        return CollectionPage.of(
                bodies,
                paging.page(),
                paging.pageSize(),
                "/vocabularies/" + vocabularyName + "/versions",
                linkQuery(params, List.of("sort")));
    }

    /** POST /vocabularies/{vocabularyName}/versions (SIMPL-18173, SIMPL-18190; D4, D7, D43, D46, D51). */
    @PostMapping("/{vocabularyName}/versions")
    public ResponseEntity<Representations.CreatedVersionBody> createVersion(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireMultipart(request);
        List<FormPart> parts = inspector.multipart(request, FILE, uploadLimit);
        List<Issue> issues = noQueryAllowed(request);
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        FormFields fields = new FormFields(parts, VERSION_FIELDS, Set.of());
        byte[] content = fields.requiredFile(FILE);
        String description = fields.requiredText("description");
        String changelog = fields.requiredText("changelog");
        issues.addAll(fields.issues());
        PathValues.failOn(issues);

        CreationResult result = service.createVersion(vocabularyName, description, changelog, content, user.creator());
        return created(result);
    }

    /** GET /vocabularies/{vocabularyName}/versions/{version} (SIMPL-18166). */
    @GetMapping(path = "/{vocabularyName}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Representations.VersionBody version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = inspector.queryIssues(inspector.query(request), Set.of());
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        int number = PathValues.checkVersion(version, issues);
        PathValues.failOn(issues);
        VocabularyVersion found = service.getVersion(vocabularyName, number);
        return representations.version(
                found, service.getVocabulary(vocabularyName).latestVersion());
    }

    /** PATCH /vocabularies/{vocabularyName}/versions/{version} (D4, D41). */
    @PatchMapping(path = "/{vocabularyName}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Representations.VersionBody updateVersionStatus(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        AuthenticatedUser user = Callers.of(request);
        inspector.requireAcceptable(request, MediaType.APPLICATION_JSON);
        inspector.requireJson(request);
        List<Issue> issues = noQueryAllowed(request);
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        int number = PathValues.checkVersion(version, issues);
        Status status = statusFromBody(inspector.jsonObject(request), issues);
        PathValues.failOn(issues);
        VocabularyVersion updated = service.updateVersionStatus(vocabularyName, number, status, user.subject());
        return representations.version(
                updated, service.getVocabulary(vocabularyName).latestVersion());
    }

    private ResponseEntity<Representations.CreatedVersionBody> created(CreationResult result) {
        Representations.CreatedVersionBody body = representations.created(result);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private List<Issue> noQueryAllowed(HttpServletRequest request) {
        return new ArrayList<>(inspector.queryIssues(inspector.query(request), Set.of()));
    }

    private static Status statusFromBody(JsonNode body, List<Issue> issues) {
        Iterator<String> names = body.propertyNames().iterator();
        while (names.hasNext()) {
            String field = names.next();
            if (!"status".equals(field)) {
                issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' is not supported.", null));
            }
        }
        JsonNode value = body.get("status");
        if (value == null || value.isNull()) {
            issues.add(Issue.schemaViolation("body", "status", "Property 'status' is required.", null));
            return null;
        }
        Status status = value.isString() ? Status.fromWire(value.asString()) : null;
        if (status == null) {
            issues.add(Issue.schemaViolation(
                    "body", "status", "Property 'status' must be one of [ACTIVE, DEPRECATED].", value.toString()));
        }
        return status;
    }

    private static List<SearchReference> searchReferences(JsonNode body, List<Issue> issues) {
        Iterator<String> names = body.propertyNames().iterator();
        while (names.hasNext()) {
            String field = names.next();
            if (!"items".equals(field)) {
                issues.add(Issue.schemaViolation("body", field, "Property '" + field + "' is not supported.", null));
            }
        }
        JsonNode items = body.get("items");
        if (items == null || !items.isArray()) {
            issues.add(
                    Issue.schemaViolation("body", "items", "Property 'items' is required and must be an array.", null));
            return List.of();
        }
        if (items.isEmpty() || items.size() > MAX_SEARCH_ITEMS) {
            issues.add(Issue.schemaViolation(
                    "body", "items", "Property 'items' must have 1 to " + MAX_SEARCH_ITEMS + " entries.", null));
            return List.of();
        }
        List<SearchReference> references = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            searchReference(items.get(i), "items[" + i + "]", issues).ifPresent(references::add);
        }
        return references;
    }

    private static java.util.Optional<SearchReference> searchReference(JsonNode item, String path, List<Issue> issues) {
        if (!item.isObject()) {
            issues.add(Issue.schemaViolation("body", path, "Each item must be an object.", null));
            return java.util.Optional.empty();
        }
        Iterator<String> names = item.propertyNames().iterator();
        while (names.hasNext()) {
            String field = names.next();
            if (!"name".equals(field) && !"version".equals(field)) {
                issues.add(Issue.schemaViolation(
                        "body", path + "." + field, "Property '" + field + "' is not supported.", null));
            }
        }
        JsonNode name = item.get("name");
        boolean valid = true;
        if (name == null || !name.isString() || !VocabularyNames.isValidName(name.asString())) {
            issues.add(Issue.schemaViolation(
                    "body", path + ".name", "Property 'name' is required and must be a valid vocabulary name.", null));
            valid = false;
        }
        Integer version = null;
        JsonNode versionNode = item.get("version");
        if (versionNode != null) {
            if (versionNode.isIntegralNumber() && versionNode.canConvertToInt() && versionNode.asInt() >= 1) {
                version = versionNode.asInt();
            } else {
                issues.add(Issue.schemaViolation(
                        "body", path + ".version", "Property 'version' must be a positive integer.", null));
                valid = false;
            }
        }
        return valid
                ? java.util.Optional.of(new SearchReference(name.asString(), version))
                : java.util.Optional.empty();
    }

    private static String single(Map<String, List<String>> params, String name) {
        List<String> values = params.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static DateFilter dateFilter(Map<String, List<String>> params, String name, List<Issue> issues) {
        String value = single(params, name);
        if (value == null) {
            return null;
        }
        DateFilter filter = DateFilter.parse(value);
        if (filter == null) {
            issues.add(Issue.schemaViolation(
                    "query",
                    name,
                    "Query parameter '" + name + "' must be a valid ISO 8601 date or date-time.",
                    value));
        }
        return filter;
    }

    private static String sortValue(Map<String, List<String>> params, List<String> allowed, List<Issue> issues) {
        String value = single(params, "sort");
        if (value != null && !allowed.contains(value)) {
            issues.add(Issue.schemaViolation(
                    "query", "sort", "Query parameter 'sort' must be one of " + allowed + ".", value));
            return null;
        }
        return value;
    }

    static Comparator<Vocabulary> vocabularyOrder(String sort) {
        Comparator<Vocabulary> byName = Comparator.comparing(Vocabulary::name);
        if (sort == null) {
            return byName;
        }
        boolean descending = sort.startsWith("-");
        String field = descending ? sort.substring(1) : sort;
        Function<Vocabulary, String> key = switch (field) {
            case "status" -> v -> v.status().name();
            case "creationDate" -> Vocabulary::creationDate;
            case "lastUpdated" -> Vocabulary::lastUpdateDate;
            default -> Vocabulary::name;
        };
        Comparator<Vocabulary> primary = Comparator.comparing(key);
        if (descending) {
            primary = primary.reversed();
        }
        return primary.thenComparing(byName);
    }

    static Comparator<VocabularyVersion> versionOrder(String sort) {
        Comparator<VocabularyVersion> newestFirst =
                Comparator.comparingInt(VocabularyVersion::version).reversed();
        if (sort == null) {
            return newestFirst;
        }
        boolean descending = sort.startsWith("-");
        String field = descending ? sort.substring(1) : sort;
        Function<VocabularyVersion, String> key = switch (field) {
            case "status" -> v -> v.status().name();
            case "creationDate" -> VocabularyVersion::creationDate;
            default -> VocabularyVersion::name;
        };
        Comparator<VocabularyVersion> primary = Comparator.comparing(key);
        if (descending) {
            primary = primary.reversed();
        }
        return primary.thenComparing(newestFirst);
    }

    private static String linkQuery(Map<String, List<String>> params, List<String> names) {
        List<String> pairs = new ArrayList<>();
        for (String name : names) {
            String value = single(params, name);
            if (value != null) {
                pairs.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return String.join("&", pairs);
    }
}

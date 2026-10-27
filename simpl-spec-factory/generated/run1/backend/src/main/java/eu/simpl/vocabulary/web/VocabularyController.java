package eu.simpl.vocabulary.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyVersion;
import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import eu.simpl.vocabulary.service.Upload;
import eu.simpl.vocabulary.service.VocabularyService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/** Internal vocabularies: list, search, create, details, status, versions. */
@RestController
@RequestMapping(path = "/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class VocabularyController {

    private static final Set<String> LIST_PARAMS = Set.of("name", "status", "creationDate", "lastUpdated", "sort",
            "page", "pageSize");
    private static final List<String> LIST_SORTS = List.of("name", "-name", "status", "-status", "creationDate",
            "-creationDate", "lastUpdated", "-lastUpdated");
    private static final Set<String> VERSION_PARAMS = Set.of("sort", "page", "pageSize");
    private static final List<String> VERSION_SORTS = List.of("name", "-name", "status", "-status", "creationDate",
            "-creationDate");
    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");

    private final VocabularyService service;
    private final ResourceMapper mapper;
    private final AppProperties props;

    public VocabularyController(VocabularyService service, ResourceMapper mapper, AppProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.props = props;
    }

    // ------------------------------------------------------------------ list (SIMPL-18162, 18164, D23, D29)

    @GetMapping("/vocabularies")
    public Dtos.PageResponse<Dtos.VocabularyCategory> list(@RequestParam MultiValueMap<String, String> query) {
        List<ProblemIssue> issues = new ArrayList<>();
        RequestChecks.onlyAllowed(query, LIST_PARAMS, issues);
        String name = query.getFirst("name");
        if (name != null && !RequestChecks.validName(name)) {
            issues.add(ProblemIssue.schemaViolation("query", "name", name,
                    "Query parameter 'name' " + RequestChecks.NAME_RULE + "."));
        }
        String statusRaw = query.getFirst("status");
        Status status = Status.parse(statusRaw);
        if (statusRaw != null && status == null) {
            issues.add(ProblemIssue.schemaViolation("query", "status", statusRaw,
                    "Query parameter 'status' must be one of [ACTIVE, DEPRECATED]."));
        }
        LocalDate created = date(query, "creationDate", issues);
        LocalDate updated = date(query, "lastUpdated", issues);
        String sort = sort(query, LIST_SORTS, issues);
        RequestChecks.Paging paging = RequestChecks.paging(query, props.maxPageSize(), issues);
        RequestChecks.throwIfAny(issues);

        String needle = name == null ? null : name.toLowerCase(Locale.ROOT);
        List<Vocabulary> matches = service.findAll().stream()
                .filter(v -> needle == null || v.name().toLowerCase(Locale.ROOT).contains(needle))
                .filter(v -> status == null || v.status() == status)
                .filter(v -> created == null || sameDay(v.creationDate(), created))
                .filter(v -> updated == null || sameDay(v.lastUpdateDate(), updated))
                .sorted(listComparator(sort))
                .toList();
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("sort", query.getFirst("sort"));
        extra.put("name", name);
        extra.put("status", statusRaw);
        extra.put("creationDate", query.getFirst("creationDate"));
        extra.put("lastUpdated", query.getFirst("lastUpdated"));
        return RequestChecks.page(matches, paging, "/vocabularies", extra, mapper::category);
    }

    // ------------------------------------------------------------------ create (SIMPL-18170, 18186, 18187)

    @PostMapping(path = "/vocabularies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Dtos.VocabularyVersionMetadataResponse> create(MultipartHttpServletRequest request,
                                                                         Authentication auth) throws IOException {
        List<ProblemIssue> issues = new ArrayList<>();
        String name = field(request, "name", issues);
        if (name != null && !RequestChecks.validName(name)) {
            issues.add(ProblemIssue.schemaViolation("body", "name", name, "Field 'name' " + RequestChecks.NAME_RULE
                    + "."));
        }
        String description = field(request, "description", issues);
        String changelog = field(request, "changelog", issues);
        rejectVersionField(request, issues);
        Upload file = file(request, issues);
        RequestChecks.throwIfAny(issues);

        VocabularyService.Created created = service.create(name, description, changelog, file,
                RequestChecks.user(auth));
        return createdResponse(created);
    }

    // ------------------------------------------------------------------ search (OpenAPI only)

    @PostMapping(path = "/vocabularies/searches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Dtos.PageResponse<Dtos.VocabularySearchItem> search(@RequestBody JsonNode body) {
        List<ProblemIssue> issues = new ArrayList<>();
        List<Map.Entry<String, Integer>> wanted = new ArrayList<>();
        if (body == null || !body.isObject()) {
            throw ApiException.badRequest(ProblemIssue.schemaViolation("body", "items", null,
                    "The request body must be a JSON object with an 'items' array."));
        }
        body.fieldNames().forEachRemaining(f -> {
            if (!f.equals("items")) {
                issues.add(ProblemIssue.schemaViolation("body", f, null, "Property '" + f + "' is not allowed."));
            }
        });
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            issues.add(ProblemIssue.schemaViolation("body", "items", null,
                    "Property 'items' is required and must be an array with at least one element."));
        } else {
            for (int i = 0; i < items.size(); i++) {
                JsonNode item = items.get(i);
                String at = "items[" + i + "]";
                if (!item.isObject()) {
                    issues.add(ProblemIssue.schemaViolation("body", at, null, at + " must be an object."));
                    continue;
                }
                Iterator<String> fields = item.fieldNames();
                while (fields.hasNext()) {
                    String f = fields.next();
                    if (!f.equals("name") && !f.equals("version")) {
                        issues.add(ProblemIssue.schemaViolation("body", at + "." + f, null,
                                "Property '" + f + "' is not allowed."));
                    }
                }
                JsonNode n = item.get("name");
                if (n == null || !n.isTextual() || !RequestChecks.validName(n.asText())) {
                    issues.add(ProblemIssue.schemaViolation("body", at + ".name", n == null ? null : n.asText(),
                            at + ".name is required and " + RequestChecks.NAME_RULE + "."));
                    continue;
                }
                JsonNode v = item.get("version");
                Integer version = null;
                if (v != null && !v.isNull()) {
                    if (!v.isIntegralNumber() || v.asLong() < 1 || v.asLong() > Integer.MAX_VALUE) {
                        issues.add(ProblemIssue.schemaViolation("body", at + ".version", v.asText(),
                                at + ".version must be an integer greater than or equal to 1."));
                        continue;
                    }
                    version = v.asInt();
                }
                wanted.add(Map.entry(n.asText(), version == null ? 0 : version));
            }
        }
        RequestChecks.throwIfAny(issues);

        List<Dtos.VocabularySearchItem> found = new ArrayList<>();
        Set<Map.Entry<String, Integer>> seen = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> w : wanted) {
            if (!seen.add(w)) {
                continue;
            }
            Optional<Vocabulary> v = service.find(w.getKey());
            if (v.isEmpty()) {
                continue;
            }
            if (w.getValue() == 0) {
                found.add(mapper.searchLatest(v.get()));
            } else {
                v.get().version(w.getValue()).ifPresent(ver -> found.add(mapper.searchVersion(v.get(), ver)));
            }
        }
        int pageSize = Math.max(20, found.size());
        return new Dtos.PageResponse<>(1, found.isEmpty() ? 0 : 1, found.size(), pageSize, null, null, null, null,
                null, found);
    }

    // ------------------------------------------------------------------ details and status

    @GetMapping("/vocabularies/{vocabularyName}")
    public Dtos.VocabularyCategory get(@PathVariable String vocabularyName) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        return mapper.category(service.get(vocabularyName));
    }

    @PatchMapping(path = "/vocabularies/{vocabularyName}", consumes = {MediaType.APPLICATION_JSON_VALUE,
            "application/merge-patch+json"})
    public Dtos.VocabularyCategory patch(@PathVariable String vocabularyName, @RequestBody JsonNode body) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        Status status = statusFromBody(body);
        return mapper.category(service.changeStatus(vocabularyName, status));
    }

    // ------------------------------------------------------------------ versions

    @GetMapping("/vocabularies/{vocabularyName}/versions")
    public Dtos.PageResponse<Dtos.VocabularyMetadata> versions(@PathVariable String vocabularyName,
                                                               @RequestParam MultiValueMap<String, String> query) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        List<ProblemIssue> issues = new ArrayList<>();
        RequestChecks.onlyAllowed(query, VERSION_PARAMS, issues);
        String sort = sort(query, VERSION_SORTS, issues);
        RequestChecks.Paging paging = RequestChecks.paging(query, props.maxPageSize(), issues);
        RequestChecks.throwIfAny(issues);
        Vocabulary vocabulary = service.get(vocabularyName);
        List<VocabularyVersion> sorted = vocabulary.versions().stream().sorted(versionComparator(sort)).toList();
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("sort", query.getFirst("sort"));
        return RequestChecks.page(sorted, paging, "/vocabularies/" + vocabularyName + "/versions", extra,
                ver -> mapper.metadata(vocabulary, ver));
    }

    @PostMapping(path = "/vocabularies/{vocabularyName}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Dtos.VocabularyVersionMetadataResponse> createVersion(
            @PathVariable String vocabularyName, MultipartHttpServletRequest request, Authentication auth)
            throws IOException {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        service.get(vocabularyName); // 404 before validating the body
        List<ProblemIssue> issues = new ArrayList<>();
        rejectVersionField(request, issues);
        String description = field(request, "description", issues);
        String changelog = field(request, "changelog", issues);
        Upload file = file(request, issues);
        RequestChecks.throwIfAny(issues);
        VocabularyService.Created created = service.createVersion(vocabularyName, description, changelog, file,
                RequestChecks.user(auth));
        return createdResponse(created);
    }

    @GetMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public Dtos.VocabularyMetadata version(@PathVariable String vocabularyName, @PathVariable String version) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        int number = RequestChecks.requireVersion(version);
        Vocabulary vocabulary = service.get(vocabularyName);
        VocabularyVersion ver = vocabulary.version(number)
                .orElseThrow(() -> VocabularyService.versionNotFound(vocabularyName, number));
        return mapper.metadata(vocabulary, ver);
    }

    @PatchMapping(path = "/vocabularies/{vocabularyName}/versions/{version}", consumes = {
            MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public Dtos.VocabularyMetadata patchVersion(@PathVariable String vocabularyName, @PathVariable String version,
                                                @RequestBody JsonNode body) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        int number = RequestChecks.requireVersion(version);
        Status status = statusFromBody(body);
        Vocabulary updated = service.changeVersionStatus(vocabularyName, number, status);
        return mapper.metadata(updated, updated.version(number).orElseThrow());
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<Dtos.VocabularyVersionMetadataResponse> createdResponse(VocabularyService.Created c) {
        Dtos.VocabularyVersionMetadataResponse body = mapper.created(c.vocabulary(), c.version(), c.warnings());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, body.href())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static Status statusFromBody(JsonNode body) {
        JsonNode s = body == null ? null : body.get("status");
        Status status = (s != null && s.isTextual()) ? Status.parse(s.asText()) : null;
        if (status == null) {
            throw ApiException.badRequest(ProblemIssue.schemaViolation("body", "status",
                    s == null || s.isNull() ? null : s.asText(),
                    "Property 'status' is required and must be one of [ACTIVE, DEPRECATED]."));
        }
        return status;
    }

    /** D8: the version is assigned by the API; a client-provided version is rejected. */
    private static void rejectVersionField(MultipartHttpServletRequest request, List<ProblemIssue> issues) {
        if (request.getParameterMap().containsKey("version") || request.getFileMap().containsKey("version")) {
            issues.add(ProblemIssue.schemaViolation("body", "version", request.getParameter("version"),
                    "Field 'version' must not be provided. The API assigns version numbers automatically."));
        }
    }

    private static String field(MultipartHttpServletRequest request, String name, List<ProblemIssue> issues)
            throws IOException {
        String value = request.getParameter(name);
        if (value == null) {
            MultipartFile asFile = request.getFile(name);
            if (asFile != null) {
                value = new String(asFile.getBytes(), StandardCharsets.UTF_8);
            }
        }
        if (value == null || value.isBlank()) {
            issues.add(ProblemIssue.schemaViolation("body", name, value, "Field '" + name + "' is required."));
            return null;
        }
        return value.trim();
    }

    /** The vocabularyFile part. Also accepted as a plain text form field. */
    static Upload file(MultipartHttpServletRequest request, List<ProblemIssue> issues) throws IOException {
        MultipartFile part = request.getFile("vocabularyFile");
        if (part != null) {
            return new Upload(part.getOriginalFilename(), part.getContentType(), part.getBytes());
        }
        String text = request.getParameter("vocabularyFile");
        if (text != null) {
            return new Upload(null, "text/plain", text.getBytes(StandardCharsets.UTF_8));
        }
        issues.add(ProblemIssue.schemaViolation("body", "vocabularyFile", null,
                "Field 'vocabularyFile' is required (a Turtle file)."));
        return null;
    }

    private static String sort(MultiValueMap<String, String> query, List<String> allowed, List<ProblemIssue> issues) {
        String sort = query.getFirst("sort");
        if (sort != null && !allowed.contains(sort)) {
            issues.add(ProblemIssue.schemaViolation("query", "sort", sort,
                    "Query parameter 'sort' must be one of [" + String.join(", ", allowed) + "]."));
            return null;
        }
        return sort;
    }

    /** creationDate / lastUpdated filter: an ISO 8601 date or date-time; matches items on the same UTC day. */
    private static LocalDate date(MultiValueMap<String, String> query, String name, List<ProblemIssue> issues) {
        String raw = query.getFirst(name);
        if (raw == null) {
            return null;
        }
        if (DATE.matcher(raw).matches()) {
            try {
                if (raw.length() == 10) {
                    return LocalDate.parse(raw);
                }
                try {
                    return OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(raw).toLocalDate();
                }
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }
        issues.add(ProblemIssue.schemaViolation("query", name, raw,
                "Query parameter '" + name + "' must be an ISO 8601 date (YYYY-MM-DD) or date-time."));
        return null;
    }

    private static boolean sameDay(Instant instant, LocalDate day) {
        return instant != null && instant.atOffset(ZoneOffset.UTC).toLocalDate().equals(day);
    }

    static Comparator<Vocabulary> listComparator(String sort) {
        Comparator<Vocabulary> byName = Comparator.comparing(Vocabulary::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Vocabulary::name);
        if (sort == null) {
            return byName;
        }
        boolean desc = sort.startsWith("-");
        Comparator<Vocabulary> c = switch (desc ? sort.substring(1) : sort) {
            case "status" -> Comparator.comparing(v -> v.status().name());
            case "creationDate" -> Comparator.comparing(Vocabulary::creationDate);
            case "lastUpdated" -> Comparator.comparing(Vocabulary::lastUpdateDate);
            default -> byName;
        };
        if (desc) {
            c = c.reversed();
        }
        return c.thenComparing(byName);
    }

    /** Default: newest version first (SIMPL-18167 "chronological list"). */
    static Comparator<VocabularyVersion> versionComparator(String sort) {
        Comparator<VocabularyVersion> byNumber = Comparator.comparingInt(VocabularyVersion::version);
        if (sort == null) {
            return byNumber.reversed();
        }
        boolean desc = sort.startsWith("-");
        Comparator<VocabularyVersion> c = switch (desc ? sort.substring(1) : sort) {
            case "status" -> Comparator.<VocabularyVersion, String>comparing(v -> v.status().name())
                    .thenComparing(byNumber);
            case "creationDate" -> Comparator.comparing(VocabularyVersion::creationDate).thenComparing(byNumber);
            default -> byNumber; // all versions share the name; order by number
        };
        return desc ? c.reversed() : c;
    }
}

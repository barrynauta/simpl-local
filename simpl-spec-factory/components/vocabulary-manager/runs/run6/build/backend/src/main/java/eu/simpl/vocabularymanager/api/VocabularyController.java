package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.InstantRange;
import eu.simpl.vocabularymanager.store.SortOrder;
import eu.simpl.vocabularymanager.store.VocabularyQuery;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Vocabulary management operations of the OpenAPI file (tag "Vocabulary management"). */
@RestController
@RequestMapping("/v1")
public class VocabularyController {

    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    private static final int MAX_SEARCH_ITEMS = 100;

    private final VocabularyService service;
    private final Representations representations;
    private final JsonBodyReader jsonReader;
    private final long maxUploadBytes;

    /**
     * Creates the controller.
     *
     * @param service the vocabulary service
     * @param representations the wire mapper
     * @param jsonReader the JSON body reader
     * @param properties the configuration
     */
    public VocabularyController(
            VocabularyService service,
            Representations representations,
            JsonBodyReader jsonReader,
            AppProperties properties) {
        this.service = service;
        this.representations = representations;
        this.jsonReader = jsonReader;
        this.maxUploadBytes = properties.limits().maxUploadBytes();
    }

    /**
     * GET /vocabularies (getVocabularies).
     *
     * @param request the request
     * @return the page of vocabularies
     */
    @GetMapping("/vocabularies")
    public ResponseEntity<PageDto<Representations.VocabularyDto>> list(HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> parameters = RequestChecks.queryParameters(request, issues);
        RequestChecks.checkQueryNames(parameters, ListParameters.VOCABULARY_LIST, issues);
        String name = ListParameters.nameFilter(parameters, issues);
        VocabularyStatus status = ListParameters.statusFilter(parameters, issues);
        InstantRange created = ListParameters.dateFilter(parameters, "creationDate", issues);
        InstantRange updated = ListParameters.dateFilter(parameters, "lastUpdated", issues);
        SortOrder sort = ListParameters.vocabularySort(parameters, issues);
        Paging paging = Paging.from(parameters, issues);
        RequestChecks.throwIfAny(issues);
        Page<Vocabulary> page = service.listVocabularies(
                new VocabularyQuery(name, status, created, updated, sort, paging.offset(), paging.pageSize()));
        return json(paging.response(
                "/vocabularies",
                ListParameters.linkParameters(
                        parameters, List.of("name", "status", "creationDate", "lastUpdated", "sort")),
                page.total(),
                page.items().stream().map(representations::vocabulary).toList()));
    }

    /**
     * POST /vocabularies (createVocabulary).
     *
     * @param request the multipart request
     * @param jwt the caller's token
     * @return 201 with the created version 1
     */
    @PostMapping("/vocabularies")
    public ResponseEntity<Representations.CreatedVersionDto> create(
            HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireMultipart(request);
        MultipartForm form = MultipartForm.read(request);
        form.checkFileSize(ContentValidator.FIELD, maxUploadBytes);
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> query = RequestChecks.queryParameters(request, issues);
        RequestChecks.checkQueryNames(query, Set.of(), issues);
        form.checkNames(CREATE_FIELDS, Set.of(), issues);
        String name = form.text("name", issues);
        if (name == null && !form.has("name")) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "name", "Field 'name' is required."));
        } else if (name != null && !RequestChecks.validName(name)) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "name", "Field 'name' must be 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
        String description = RequestChecks.requiredText("description", form.text("description", issues), issues);
        String changelog = RequestChecks.requiredText("changelog", form.text("changelog", issues), issues);
        byte[] content = form.file(ContentValidator.FIELD, issues);
        RequestChecks.throwIfAny(issues);
        VocabularyService.Created created =
                service.createVocabulary(name, description, changelog, content, Callers.creator(jwt));
        return createdResponse(created);
    }

    /**
     * POST /vocabularies/searches (searchVocabularies), decision D38.
     *
     * @param request the JSON request
     * @return the results, unpaged
     */
    @PostMapping("/vocabularies/searches")
    public ResponseEntity<PageDto<Representations.SearchItemDto>> search(HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireJson(request);
        JsonNode body = jsonReader.read(request);
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> query = RequestChecks.queryParameters(request, issues);
        RequestChecks.checkQueryNames(query, Set.of(), issues);
        List<VocabularyService.SearchReference> references = searchReferences(body, issues);
        RequestChecks.throwIfAny(issues);
        List<Representations.SearchItemDto> items = service.search(references).stream()
                .map(representations::searchItem)
                .toList();
        return json(PageDto.unpaged(items));
    }

    private static List<VocabularyService.SearchReference> searchReferences(JsonNode body, List<Issue> issues) {
        if (!JsonBodyReader.checkObject(body, "", Set.of("items"), issues)) {
            return List.of();
        }
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > MAX_SEARCH_ITEMS) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "items", "Field 'items' must be an array of 1 to " + MAX_SEARCH_ITEMS + " items."));
            return List.of();
        }
        LinkedHashSet<VocabularyService.SearchReference> references = new LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String path = "items[" + i + "]";
            if (!JsonBodyReader.checkObject(item, path, Set.of("name", "version"), issues)) {
                continue;
            }
            JsonNode name = item.get("name");
            JsonNode version = item.get("version");
            boolean ok = true;
            if (name == null || !name.isString() || !RequestChecks.validName(name.asString())) {
                issues.add(
                        Issue.schemaViolation(Issue.IN_BODY, path + ".name", "A valid vocabulary name is required."));
                ok = false;
            }
            Long versionNumber = null;
            if (version != null) {
                if (!version.isIntegralNumber() || version.bigIntegerValue().signum() <= 0) {
                    issues.add(Issue.schemaViolation(
                            Issue.IN_BODY, path + ".version", "Field 'version' must be an integer of at least 1."));
                    ok = false;
                } else {
                    BigInteger big = version.bigIntegerValue();
                    versionNumber = big.bitLength() < 63 ? big.longValue() : Long.MAX_VALUE;
                }
            }
            if (ok) {
                references.add(new VocabularyService.SearchReference(name.asString(), versionNumber));
            }
        }
        return List.copyOf(references);
    }

    /**
     * GET /vocabularies/{vocabularyName} (getVocabulary).
     *
     * @param vocabularyName the name
     * @param request the request
     * @return the vocabulary
     */
    @GetMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<Representations.VocabularyDto> get(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        RequestChecks.throwIfAny(issues);
        return json(representations.vocabulary(service.getVocabulary(vocabularyName)));
    }

    /**
     * PATCH /vocabularies/{vocabularyName} (updateVocabularyStatus).
     *
     * @param vocabularyName the name
     * @param request the JSON request
     * @param jwt the caller's token
     * @return the vocabulary after the change
     */
    @PatchMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<Representations.VocabularyDto> changeStatus(
            @PathVariable String vocabularyName, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireJson(request);
        JsonNode body = jsonReader.read(request);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        VocabularyStatus status = StatusBody.read(body, issues);
        RequestChecks.throwIfAny(issues);
        Vocabulary changed = service.changeVocabularyStatus(vocabularyName, status, Callers.creator(jwt));
        return json(representations.vocabulary(changed));
    }

    /**
     * GET /vocabularies/{vocabularyName}/versions (getVocabularyVersions).
     *
     * @param vocabularyName the name
     * @param request the request
     * @return the page of versions
     */
    @GetMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<PageDto<Representations.VersionDto>> versions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> parameters = RequestChecks.queryParameters(request, issues);
        RequestChecks.checkQueryNames(parameters, ListParameters.VERSION_LIST, issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        SortOrder sort = ListParameters.versionSort(parameters, issues);
        Paging paging = Paging.from(parameters, issues);
        RequestChecks.throwIfAny(issues);
        Vocabulary vocabulary = service.getVocabulary(vocabularyName);
        Page<VocabularyVersion> page = service.listVersions(vocabularyName, sort, paging.offset(), paging.pageSize());
        return json(paging.response(
                "/vocabularies/" + vocabularyName + "/versions",
                ListParameters.linkParameters(parameters, List.of("sort")),
                page.total(),
                page.items().stream()
                        .map(v -> representations.version(v, vocabulary.latestVersion()))
                        .toList()));
    }

    /**
     * POST /vocabularies/{vocabularyName}/versions (createVocabularyVersion).
     *
     * @param vocabularyName the name
     * @param request the multipart request
     * @param jwt the caller's token
     * @return 201 with the created version
     */
    @PostMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<Representations.CreatedVersionDto> createVersion(
            @PathVariable String vocabularyName, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireMultipart(request);
        MultipartForm form = MultipartForm.read(request);
        form.checkFileSize(ContentValidator.FIELD, maxUploadBytes);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        if (form.has("version")) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "version", "The version number is assigned automatically; do not send 'version'."));
        }
        form.checkNames(VERSION_FIELDS, Set.of(), issues);
        issues.removeIf(i -> "version".equals(i.name()) && i.type().endsWith("unknownInput"));
        String description = RequestChecks.requiredText("description", form.text("description", issues), issues);
        String changelog = RequestChecks.requiredText("changelog", form.text("changelog", issues), issues);
        byte[] content = form.file(ContentValidator.FIELD, issues);
        RequestChecks.throwIfAny(issues);
        VocabularyService.Created created =
                service.createVersion(vocabularyName, description, changelog, content, Callers.creator(jwt));
        return createdResponse(created);
    }

    /**
     * GET /vocabularies/{vocabularyName}/versions/{version} (getVocabularyVersion).
     *
     * @param vocabularyName the name
     * @param version the version number
     * @param request the request
     * @return the version
     */
    @GetMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<Representations.VersionDto> getVersion(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        RequestChecks.checkPathVersion(version, issues);
        RequestChecks.throwIfAny(issues);
        Vocabulary vocabulary = service.getVocabulary(vocabularyName);
        VocabularyVersion found = service.getVersion(vocabularyName, version);
        return json(representations.version(found, vocabulary.latestVersion()));
    }

    /**
     * PATCH /vocabularies/{vocabularyName}/versions/{version} (updateVocabularyVersionStatus).
     *
     * @param vocabularyName the name
     * @param version the version number
     * @param request the JSON request
     * @param jwt the caller's token
     * @return the version after the change
     */
    @PatchMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<Representations.VersionDto> changeVersionStatus(
            @PathVariable String vocabularyName,
            @PathVariable String version,
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireJson(request);
        JsonNode body = jsonReader.read(request);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        RequestChecks.checkPathVersion(version, issues);
        VocabularyStatus status = StatusBody.read(body, issues);
        RequestChecks.throwIfAny(issues);
        VocabularyVersion changed = service.changeVersionStatus(vocabularyName, version, status, Callers.creator(jwt));
        Vocabulary vocabulary = service.getVocabulary(vocabularyName);
        return json(representations.version(changed, vocabulary.latestVersion()));
    }

    private ResponseEntity<Representations.CreatedVersionDto> createdResponse(VocabularyService.Created created) {
        String location = Representations.versionHref(
                created.version().name(), created.version().version());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", location)
                .contentType(MediaType.APPLICATION_JSON)
                .body(representations.created(created));
    }

    private static <T> ResponseEntity<T> json(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}

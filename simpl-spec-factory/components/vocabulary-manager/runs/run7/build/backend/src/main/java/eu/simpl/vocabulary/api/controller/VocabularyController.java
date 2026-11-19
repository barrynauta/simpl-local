package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.ApiMapper;
import eu.simpl.vocabulary.api.dto.CreatedVersionDto;
import eu.simpl.vocabulary.api.dto.PageDto;
import eu.simpl.vocabulary.api.dto.VersionDto;
import eu.simpl.vocabulary.api.dto.VocabularyDto;
import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.api.request.MultipartForm;
import eu.simpl.vocabulary.api.request.QueryParameters;
import eu.simpl.vocabulary.api.request.QueryRules;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.security.Roles;
import eu.simpl.vocabulary.service.CreatedVersion;
import eu.simpl.vocabulary.service.Page;
import eu.simpl.vocabulary.service.VersionView;
import eu.simpl.vocabulary.service.VocabularyQuery;
import eu.simpl.vocabulary.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/** Operations on vocabularies, versions and content (SIMPL-18162 to SIMPL-18196, decisions D3 to D81). */
@RestController
@RequestMapping("/v1")
public class VocabularyController {

    private static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMETERS = Set.of("sort", "page", "pageSize");
    private static final Set<String> CREATE_FIELDS =
            Set.of(MultipartForm.FILE_FIELD, "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS =
            Set.of(MultipartForm.FILE_FIELD, "description", "changelog", "version");

    private final VocabularyService service;
    private final ApiMapper mapper;
    private final AppProperties.Limits limits;

    public VocabularyController(VocabularyService service, ApiMapper mapper, AppProperties properties) {
        this.service = service;
        this.mapper = mapper;
        this.limits = properties.limits();
    }

    @GetMapping("/vocabularies")
    public ResponseEntity<PageDto<VocabularyDto>> list(HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        List<Issue> issues = new ArrayList<>(query.allowOnly(LIST_PARAMETERS));
        VocabularyQuery vocabularyQuery = new VocabularyQuery(
                QueryRules.nameFilter(query, issues),
                QueryRules.status(query, issues),
                QueryRules.date(query, "creationDate", issues),
                QueryRules.date(query, "lastUpdated", issues),
                QueryRules.sort(query, QueryRules.VOCABULARY_SORTS, issues),
                QueryRules.page(query, issues),
                QueryRules.pageSize(query, issues));
        Requests.rejectIfAny(issues);
        Page<Vocabulary> page = service.listVocabularies(vocabularyQuery);
        return Requests.json(200, ApiMapper.page("/vocabularies", query, page, mapper::vocabulary));
    }

    @PostMapping("/vocabularies")
    public ResponseEntity<CreatedVersionDto> create(HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        MultipartForm form = MultipartForm.read(request, limits.maxFileBytes(), limits.maxRequestBytes());
        issues.addAll(form.unknownOrRepeated(CREATE_FIELDS, Set.of()));
        String name = Requests.text(form, "name");
        if (name == null) {
            issues.add(FieldRules.required("name"));
        } else {
            FieldRules.checkName(Issue.IN_BODY, "name", name).ifPresent(issues::add);
        }
        String description = Requests.text(form, "description");
        FieldRules.checkRequiredText("description", description).ifPresent(issues::add);
        String changelog = Requests.text(form, "changelog");
        FieldRules.checkRequiredText("changelog", changelog).ifPresent(issues::add);
        Optional<MultipartForm.FormPart> file = form.first(MultipartForm.FILE_FIELD);
        if (file.isEmpty()) {
            issues.add(FieldRules.required(MultipartForm.FILE_FIELD));
        }
        Requests.rejectIfAny(issues);
        CreatedVersion created = service.createVocabulary(
                name, description, changelog, file.get().bytes(), Roles.creator());
        return created(created);
    }

    @GetMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<VocabularyDto> get(@PathVariable String vocabularyName, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        Requests.rejectIfAny(issues);
        return Requests.json(200, mapper.vocabulary(service.getVocabulary(vocabularyName)));
    }

    @PatchMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<VocabularyDto> updateStatus(@PathVariable String vocabularyName, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        JsonNode body = Requests.readJson(request, limits.maxJsonBytes(), issues);
        VocabularyStatus status = Requests.statusBody(body, issues);
        Requests.rejectIfAny(issues);
        return Requests.json(200, mapper.vocabulary(service.updateVocabularyStatus(vocabularyName, status)));
    }

    @GetMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<PageDto<VersionDto>> versions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        List<Issue> issues = new ArrayList<>(query.allowOnly(VERSION_LIST_PARAMETERS));
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        String sort = QueryRules.sort(query, QueryRules.VERSION_SORTS, issues);
        long page = QueryRules.page(query, issues);
        int pageSize = QueryRules.pageSize(query, issues);
        Requests.rejectIfAny(issues);
        VocabularyService.VersionPage versions = service.listVersions(vocabularyName, sort, page, pageSize);
        long latest = versions.vocabulary().latestVersion();
        return Requests.json(
                200,
                ApiMapper.page(
                        "/vocabularies/" + vocabularyName + "/versions",
                        query,
                        versions.page(),
                        v -> mapper.version(v, latest)));
    }

    @PostMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<CreatedVersionDto> createVersion(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        MultipartForm form = MultipartForm.read(request, limits.maxFileBytes(), limits.maxRequestBytes());
        issues.addAll(form.unknownOrRepeated(VERSION_FIELDS, Set.of()));
        if (form.has("version")) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY,
                    "version",
                    "The version number is assigned automatically; do not send a 'version' field."));
        }
        String description = Requests.text(form, "description");
        FieldRules.checkRequiredText("description", description).ifPresent(issues::add);
        String changelog = Requests.text(form, "changelog");
        FieldRules.checkRequiredText("changelog", changelog).ifPresent(issues::add);
        Optional<MultipartForm.FormPart> file = form.first(MultipartForm.FILE_FIELD);
        if (file.isEmpty()) {
            issues.add(FieldRules.required(MultipartForm.FILE_FIELD));
        }
        Requests.rejectIfAny(issues);
        CreatedVersion created = service.createVersion(
                vocabularyName, description, changelog, file.get().bytes(), Roles.creator());
        return created(created);
    }

    @GetMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<VersionDto> version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        long number = Requests.checkPathVersion(version, issues);
        Requests.rejectIfAny(issues);
        VersionView view = service.getVersion(vocabularyName, number);
        return Requests.json(
                200, mapper.version(view.version(), view.vocabulary().latestVersion()));
    }

    @PatchMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<VersionDto> updateVersionStatus(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        long number = Requests.checkPathVersion(version, issues);
        JsonNode body = Requests.readJson(request, limits.maxJsonBytes(), issues);
        VocabularyStatus status = Requests.statusBody(body, issues);
        Requests.rejectIfAny(issues);
        VersionView view = service.updateVersionStatus(vocabularyName, number, status);
        return Requests.json(
                200, mapper.version(view.version(), view.vocabulary().latestVersion()));
    }

    @GetMapping(value = "/content/{vocabularyName}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> latestContent(@PathVariable String vocabularyName, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        Requests.rejectIfAny(issues);
        StoredContent content = service.latestContent(vocabularyName);
        return Requests.turtle(content.bytes(), vocabularyName + ".ttl");
    }

    @GetMapping(value = "/content/{vocabularyName}/versions/{version}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> versionContent(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("vocabularyName", vocabularyName, issues);
        long number = Requests.checkPathVersion(version, issues);
        Requests.rejectIfAny(issues);
        StoredContent content = service.content(vocabularyName, number);
        return Requests.turtle(content.bytes(), vocabularyName + "-v" + number + ".ttl");
    }

    private ResponseEntity<CreatedVersionDto> created(CreatedVersion created) {
        CreatedVersionDto body = mapper.created(created);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}

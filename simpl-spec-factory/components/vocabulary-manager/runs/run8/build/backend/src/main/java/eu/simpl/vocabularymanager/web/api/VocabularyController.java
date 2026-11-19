package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService.Created;
import eu.simpl.vocabularymanager.service.VocabularyService.SearchReference;
import eu.simpl.vocabularymanager.service.VocabularyService.SearchResult;
import eu.simpl.vocabularymanager.store.VocabularyFilter;
import eu.simpl.vocabularymanager.web.api.Representations.CollectionPage;
import eu.simpl.vocabularymanager.web.api.Representations.VocabularyCategory;
import eu.simpl.vocabularymanager.web.api.Representations.VocabularyMetadata;
import eu.simpl.vocabularymanager.web.api.Representations.VocabularySearchItem;
import eu.simpl.vocabularymanager.web.api.Representations.VocabularyVersionMetadataResponse;
import eu.simpl.vocabularymanager.web.body.JsonBody;
import eu.simpl.vocabularymanager.web.body.MultipartForm;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.routing.QueryParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Vocabulary management operations of the OpenAPI (tag 'Vocabulary management'). */
@RestController
public class VocabularyController {

    private static final Set<String> CREATE_FIELDS = Set.of("vocabularyFile", "name", "description", "changelog");
    private static final Set<String> VERSION_FIELDS = Set.of("vocabularyFile", "description", "changelog");
    private static final int MAX_SEARCH_ITEMS = 100;

    private final VocabularyService service;
    private final Responses responses;

    /**
     * @param service the vocabulary service
     * @param responses response builder
     */
    public VocabularyController(VocabularyService service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    /**
     * getVocabularies.
     *
     * @param request the request
     * @return the page
     */
    @GetMapping("/vocabularies")
    public ResponseEntity<byte[]> getVocabularies(HttpServletRequest request) {
        QueryParameters parameters = QueryParameters.parse(request.getQueryString());
        List<ProblemIssue> issues = new ArrayList<>();
        VocabularyFilter filter = ListQuery.vocabularyFilter(parameters, issues);
        String sort = ListQuery.vocabularySort(parameters, issues);
        Paging paging = Paging.parse(parameters, issues);
        Responses.failOn(issues);
        Page<Vocabulary> page = service.list(filter, sort, paging.offset(), paging.pageSize());
        List<VocabularyCategory> items = page.items().stream()
                .map(vocabulary -> VocabularyCategory.of(vocabulary, service.contentBase()))
                .toList();
        return responses.ok(paging.page("/vocabularies", parameters, page.total(), items));
    }

    /**
     * createVocabulary.
     *
     * @param request the multipart request
     * @return 201 with version 1
     */
    @PostMapping("/vocabularies")
    public ResponseEntity<byte[]> createVocabulary(HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request, CREATE_FIELDS, Set.of());
        List<ProblemIssue> issues = new ArrayList<>();
        String name = form.text("name");
        if (name == null) {
            issues.add(required("name"));
        } else if (!FieldRules.isValidName(name)) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", "name", "Name must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
        String description = requiredText(form, "description", issues);
        String changelog = requiredText(form, "changelog", issues);
        if (!form.has("vocabularyFile")) {
            issues.add(required("vocabularyFile"));
        }
        Responses.failOn(issues);
        Created created = service.create(
                new VocabularyService.NewVocabulary(name, description, changelog, form.bytes("vocabularyFile")),
                Responses.currentUser());
        return createdResponse(created);
    }

    /**
     * searchVocabularies (D38, D68).
     *
     * @param request the JSON request
     * @return the results, not paged
     */
    @PostMapping("/vocabularies/searches")
    public ResponseEntity<byte[]> searchVocabularies(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>(JsonBody.unknownProperties(body, Set.of("items"), ""));
        List<SearchReference> references = new ArrayList<>();
        JsonNode items = body.get("items");
        if (items == null) {
            issues.add(required("items"));
        } else if (!items.isArray() || items.isEmpty() || items.size() > MAX_SEARCH_ITEMS) {
            issues.add(ProblemIssue.schemaViolation("body", "items", "items must be an array of 1 to 100 entries."));
        } else {
            for (int i = 0; i < items.size(); i++) {
                searchReference(items.get(i), "items[" + i + "]", issues, references);
            }
        }
        Responses.failOn(issues);
        List<VocabularySearchItem> results = new ArrayList<>();
        for (SearchResult result : service.search(references)) {
            results.add(searchItem(result));
        }
        return responses.ok(new CollectionPage<>(
                1, results.isEmpty() ? 0 : 1, results.size(), 100, null, null, null, null, null, results));
    }

    /**
     * getVocabulary.
     *
     * @param vocabularyName the name
     * @return the vocabulary
     */
    @GetMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<byte[]> getVocabulary(@PathVariable String vocabularyName) {
        return responses.ok(VocabularyCategory.of(service.get(vocabularyName), service.contentBase()));
    }

    /**
     * updateVocabularyStatus.
     *
     * @param vocabularyName the name
     * @param request the JSON request
     * @return the vocabulary after the change
     */
    @PatchMapping("/vocabularies/{vocabularyName}")
    public ResponseEntity<byte[]> updateVocabularyStatus(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        VocabularyStatus status = statusFromBody(request);
        Vocabulary vocabulary = service.changeStatus(vocabularyName, status, Responses.currentUser());
        return responses.ok(VocabularyCategory.of(vocabulary, service.contentBase()));
    }

    /**
     * getVocabularyVersions.
     *
     * @param vocabularyName the name
     * @param request the request
     * @return the page of versions
     */
    @GetMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<byte[]> getVocabularyVersions(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        QueryParameters parameters = QueryParameters.parse(request.getQueryString());
        List<ProblemIssue> issues = new ArrayList<>();
        String sort = ListQuery.versionSort(parameters, issues);
        Paging paging = Paging.parse(parameters, issues);
        Responses.failOn(issues);
        Page<VocabularyVersion> page =
                service.versions(vocabularyName, sort, paging.offset(), paging.pageSize());
        List<VocabularyMetadata> items = page.items().stream()
                .map(version -> VocabularyMetadata.of(version, service.contentBase()))
                .toList();
        return responses.ok(
                paging.page("/vocabularies/" + vocabularyName + "/versions", parameters, page.total(), items));
    }

    /**
     * createVocabularyVersion.
     *
     * @param vocabularyName the name
     * @param request the multipart request
     * @return 201 with the new version
     */
    @PostMapping("/vocabularies/{vocabularyName}/versions")
    public ResponseEntity<byte[]> createVocabularyVersion(
            @PathVariable String vocabularyName, HttpServletRequest request) {
        MultipartForm form = MultipartForm.read(request, VERSION_FIELDS, Set.of());
        List<ProblemIssue> issues = new ArrayList<>();
        String description = requiredText(form, "description", issues);
        String changelog = requiredText(form, "changelog", issues);
        if (!form.has("vocabularyFile")) {
            issues.add(required("vocabularyFile"));
        }
        Responses.failOn(issues);
        Created created = service.createVersion(
                new VocabularyService.NewVersion(
                        vocabularyName, description, changelog, form.bytes("vocabularyFile")),
                Responses.currentUser());
        return createdResponse(created);
    }

    /**
     * getVocabularyVersion.
     *
     * @param vocabularyName the name
     * @param version the version number
     * @return the version
     */
    @GetMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> getVocabularyVersion(
            @PathVariable String vocabularyName, @PathVariable String version) {
        VocabularyVersion found = service.getVersion(vocabularyName, Long.parseLong(version));
        return responses.ok(VocabularyMetadata.of(found, service.contentBase()));
    }

    /**
     * updateVocabularyVersionStatus.
     *
     * @param vocabularyName the name
     * @param version the version number
     * @param request the JSON request
     * @return the version after the change
     */
    @PatchMapping("/vocabularies/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> updateVocabularyVersionStatus(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        VocabularyStatus status = statusFromBody(request);
        VocabularyVersion changed = service.changeVersionStatus(
                vocabularyName, Long.parseLong(version), status, Responses.currentUser());
        return responses.ok(VocabularyMetadata.of(changed, service.contentBase()));
    }

    private ResponseEntity<byte[]> createdResponse(Created created) {
        VocabularyVersionMetadataResponse body =
                VocabularyVersionMetadataResponse.of(created.version(), created.warnings(), service.contentBase());
        return responses.created(body, body.href());
    }

    private VocabularySearchItem searchItem(SearchResult result) {
        String href = result.latest()
                ? "/vocabularies/" + result.name()
                : "/vocabularies/" + result.name() + "/versions/" + result.version();
        String contentHref = result.latest()
                ? service.contentBase() + "/content/" + result.name()
                : service.contentBase() + "/content/" + result.name() + "/versions/" + result.version();
        return new VocabularySearchItem(
                href, contentHref, result.name(), result.version(), result.status(), result.latest());
    }

    private static void searchReference(
            JsonNode item, String path, List<ProblemIssue> issues, List<SearchReference> references) {
        if (!item.isObject()) {
            issues.add(ProblemIssue.schemaViolation("body", path, "Each item must be an object."));
            return;
        }
        issues.addAll(JsonBody.unknownProperties(item, Set.of("name", "version"), path + "."));
        JsonNode name = item.get("name");
        boolean valid = true;
        if (name == null || !name.isString() || !FieldRules.isValidName(name.asString())) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", path + ".name", "name must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
            valid = false;
        }
        JsonNode version = item.get("version");
        Long versionNumber = null;
        if (version != null) {
            if (version.isIntegralNumber() && version.canConvertToLong() && version.asLong() >= 1) {
                versionNumber = version.asLong();
            } else {
                issues.add(ProblemIssue.schemaViolation(
                        "body", path + ".version", "version must be an integer of at least 1."));
                valid = false;
            }
        }
        if (valid) {
            references.add(new SearchReference(name.asString(), versionNumber));
        }
    }

    private static VocabularyStatus statusFromBody(HttpServletRequest request) {
        JsonNode body = JsonBody.readObject(request);
        List<ProblemIssue> issues = new ArrayList<>(JsonBody.unknownProperties(body, Set.of("status"), ""));
        JsonNode status = body.get("status");
        VocabularyStatus parsed = null;
        if (status == null) {
            issues.add(required("status"));
        } else {
            parsed = status.isString() ? VocabularyStatus.parse(status.asString()).orElse(null) : null;
            if (parsed == null) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", "status", "status must be one of [ACTIVE, DEPRECATED]."));
            }
        }
        Responses.failOn(issues);
        return parsed;
    }

    private static String requiredText(MultipartForm form, String field, List<ProblemIssue> issues) {
        String value = form.text(field);
        if (value == null) {
            issues.add(required(field));
            return null;
        }
        if (!FieldRules.isValidRequiredText(value)) {
            issues.add(ProblemIssue.schemaViolation(
                    "body", field, field + " must have 1 to 1000 characters after trimming."));
            return null;
        }
        return value.strip();
    }

    static ProblemIssue required(String field) {
        return ProblemIssue.schemaViolation("body", field, field + " is required.");
    }
}

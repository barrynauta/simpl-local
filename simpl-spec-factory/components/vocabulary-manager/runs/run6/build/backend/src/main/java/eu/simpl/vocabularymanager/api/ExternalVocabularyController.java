package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations of the OpenAPI file (decisions D30, D38, D41, D42, D56). */
@RestController
@RequestMapping("/v1")
public class ExternalVocabularyController {

    private static final Set<String> REGISTER_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final int MAX_ALIASES = 20;
    private static final int MAX_SEARCH_URIS = 100;

    private final ExternalVocabularyService service;
    private final Representations representations;
    private final JsonBodyReader jsonReader;
    private final long maxUploadBytes;

    /**
     * Creates the controller.
     *
     * @param service the external vocabulary service
     * @param representations the wire mapper
     * @param jsonReader the JSON body reader
     * @param properties the configuration
     */
    public ExternalVocabularyController(
            ExternalVocabularyService service,
            Representations representations,
            JsonBodyReader jsonReader,
            AppProperties properties) {
        this.service = service;
        this.representations = representations;
        this.jsonReader = jsonReader;
        this.maxUploadBytes = properties.limits().maxUploadBytes();
    }

    /**
     * GET /external-vocabularies (getExternalVocabularies): name ascending, paged.
     *
     * @param request the request
     * @return the page
     */
    @GetMapping("/external-vocabularies")
    public ResponseEntity<PageDto<Representations.ExternalDto>> list(HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> parameters = RequestChecks.queryParameters(request, issues);
        RequestChecks.checkQueryNames(parameters, ListParameters.EXTERNAL_LIST, issues);
        Paging paging = Paging.from(parameters, issues);
        RequestChecks.throwIfAny(issues);
        Page<ExternalVocabulary> page = service.list(paging.offset(), paging.pageSize());
        return json(paging.response(
                "/external-vocabularies",
                Map.of(),
                page.total(),
                page.items().stream().map(representations::external).toList()));
    }

    /**
     * POST /external-vocabularies (createExternalVocabulary): register.
     *
     * @param request the multipart request
     * @param jwt the caller's token
     * @return 201 with the external vocabulary
     */
    @PostMapping("/external-vocabularies")
    public ResponseEntity<Representations.ExternalDto> register(
            HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireMultipart(request);
        MultipartForm form = MultipartForm.read(request);
        form.checkFileSize(ContentValidator.FIELD, maxUploadBytes);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        form.checkNames(REGISTER_FIELDS, Set.of("aliasUris"), issues);
        String name = form.text("name", issues);
        if (name == null && !form.has("name")) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "name", "Field 'name' is required."));
        } else if (name != null && !RequestChecks.validName(name)) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "name", "Field 'name' must be 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
        String namespaceUri = form.text("namespaceUri", issues);
        if (namespaceUri == null && !form.has("namespaceUri")) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "namespaceUri", "Field 'namespaceUri' is required."));
        } else if (namespaceUri != null && !UriValues.isAbsoluteUri(namespaceUri)) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "namespaceUri", "Field 'namespaceUri' must be an absolute URI."));
        }
        List<String> aliases = form.texts("aliasUris", issues);
        checkAliases(aliases, namespaceUri, issues);
        String description = optionalDescription(form.text("description", issues), issues);
        String sourceUri = form.text("sourceUri", issues);
        if (sourceUri != null && !UriValues.isAbsoluteUri(sourceUri)) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "sourceUri", "Field 'sourceUri' must be an absolute URI."));
        }
        byte[] content = form.file(ContentValidator.FIELD, issues);
        RequestChecks.throwIfAny(issues);
        ExternalVocabulary registered =
                service.register(name, namespaceUri, aliases, sourceUri, description, content, Callers.creator(jwt));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", Representations.externalHref(registered.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representations.external(registered));
    }

    private static void checkAliases(List<String> aliases, String namespaceUri, List<Issue> issues) {
        if (aliases.size() > MAX_ALIASES) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "aliasUris", "At most " + MAX_ALIASES + " alias URIs are allowed."));
            return;
        }
        Set<String> seen = new HashSet<>();
        if (namespaceUri != null) {
            seen.add(ExternalVocabulary.normalise(namespaceUri));
        }
        for (String alias : aliases) {
            if (!UriValues.isAbsoluteUri(alias)) {
                issues.add(
                        Issue.schemaViolation(Issue.IN_BODY, "aliasUris", "Every alias URI must be an absolute URI."));
                return;
            }
            if (!seen.add(ExternalVocabulary.normalise(alias))) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY,
                        "aliasUris",
                        "An alias URI repeats the namespace URI or another alias: '" + alias + "'."));
                return;
            }
        }
    }

    private static String optionalDescription(String value, List<Issue> issues) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > RequestChecks.MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "description", "Field 'description' must be at most 1000 characters."));
            return null;
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * GET /external-vocabularies/{name} (getExternalVocabulary).
     *
     * @param name the name
     * @param request the request
     * @return the external vocabulary
     */
    @GetMapping("/external-vocabularies/{name}")
    public ResponseEntity<Representations.ExternalDto> get(@PathVariable String name, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        checkPathAndNoQuery(name, request);
        return json(representations.external(service.get(name)));
    }

    /**
     * DELETE /external-vocabularies/{name} (deleteExternalVocabulary).
     *
     * @param name the name
     * @param request the request
     * @param jwt the caller's token
     * @return 204
     */
    @DeleteMapping("/external-vocabularies/{name}")
    public ResponseEntity<Void> delete(
            @PathVariable String name, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        checkPathAndNoQuery(name, request);
        service.remove(name, Callers.creator(jwt));
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /external-vocabularies/{name}/content (getExternalVocabularyContentByName).
     *
     * @param name the name
     * @param request the request
     * @return the Turtle bytes
     */
    @GetMapping("/external-vocabularies/{name}/content")
    public ResponseEntity<byte[]> contentByName(@PathVariable String name, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, ContentController.TURTLE);
        checkPathAndNoQuery(name, request);
        return ContentController.turtle(service.contentByName(name), name + ".ttl");
    }

    /**
     * POST /external-vocabularies/content (downloadExternalVocabularyContent) by namespace URI.
     *
     * @param request the JSON request
     * @return the Turtle bytes
     */
    @PostMapping("/external-vocabularies/content")
    public ResponseEntity<byte[]> contentByNamespace(HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, ContentController.TURTLE);
        RequestChecks.requireJson(request);
        JsonNode body = jsonReader.read(request);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        String namespaceUri = null;
        if (JsonBodyReader.checkObject(body, "", Set.of("namespaceUri"), issues)) {
            JsonNode node = body.get("namespaceUri");
            if (node == null || !node.isString() || !UriValues.isAbsoluteUri(node.asString())) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY, "namespaceUri", "Field 'namespaceUri' must be an absolute URI."));
            } else {
                namespaceUri = node.asString();
            }
        }
        RequestChecks.throwIfAny(issues);
        ExternalVocabulary external = service.byNamespace(namespaceUri);
        return ContentController.turtle(service.contentByName(external.name()), external.name() + ".ttl");
    }

    /**
     * POST /external-vocabulary/searches (searchExternalVocabularies), decision D38.
     *
     * @param request the JSON request
     * @return the matches, unpaged
     */
    @PostMapping("/external-vocabulary/searches")
    public ResponseEntity<PageDto<Representations.ExternalDto>> search(HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, MediaType.APPLICATION_JSON);
        RequestChecks.requireJson(request);
        JsonNode body = jsonReader.read(request);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        List<String> uris = ExternalSearchBody.read(body, MAX_SEARCH_URIS, issues);
        RequestChecks.throwIfAny(issues);
        List<Representations.ExternalDto> items =
                service.search(uris).stream().map(representations::external).toList();
        return json(PageDto.unpaged(items));
    }

    private static void checkPathAndNoQuery(String name, HttpServletRequest request) {
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("name", name, issues);
        RequestChecks.throwIfAny(issues);
    }

    private static <T> ResponseEntity<T> json(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}

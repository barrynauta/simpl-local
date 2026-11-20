package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.UploadLimiter;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.QueryParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** External vocabularies (OpenAPI tag "External vocabulary management", D41). */
@RestController
public class ExternalVocabularyController {

    static final int MAX_ALIASES = 20;
    private static final String JSON = VocabularyController.JSON;
    private static final Set<String> FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
    private static final Set<String> SINGLE_FIELDS =
            Set.of("vocabularyFile", "name", "namespaceUri", "description", "sourceUri");

    private final ExternalVocabularyService service;
    private final Representations representations;
    private final UploadLimiter limiter;
    private final JsonBody jsonBody;

    public ExternalVocabularyController(
            ExternalVocabularyService service,
            Representations representations,
            UploadLimiter limiter,
            JsonBody jsonBody) {
        this.service = service;
        this.representations = representations;
        this.limiter = limiter;
        this.jsonBody = jsonBody;
    }

    @GetMapping(value = "/v1/external-vocabularies", produces = JSON)
    public Map<String, Object> getExternalVocabularies(HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString(), Set.of("page", "pageSize"));
        RequestChecks checks = RequestChecks.start();
        long page = VocabularyController.paging(query, "page", Integer.MAX_VALUE, 1, checks);
        int size = (int) VocabularyController.paging(query, "pageSize", 100, 20, checks);
        checks.throwIfAny();
        Page<ExternalVocabulary> result = service.list(page, size);
        return representations.page(
                "/external-vocabularies",
                query.inOrder(),
                page,
                size,
                result.total(),
                result.items().stream().map(representations::external).toList());
    }

    @PostMapping(value = "/v1/external-vocabularies", produces = JSON, consumes = VocabularyController.MULTIPART)
    public ResponseEntity<Map<String, Object>> createExternalVocabulary(
            HttpServletRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        try (UploadLimiter.Permit permit = limiter.acquire()) {
            MultipartForm form = MultipartForm.read(request);
            RequestChecks checks = RequestChecks.start().noQuery(request.getQueryString());
            checks.addAll(form.unknownFields(FIELDS)).addAll(form.repeatedFields(SINGLE_FIELDS));
            String name = checks.name("name", form.text("name"));
            String namespace = checks.uri("namespaceUri", form.text("namespaceUri"), true);
            List<String> aliases = aliases(form.texts("aliasUris"), namespace, checks);
            String description = checks.optionalText("description", form.text("description"));
            String source = checks.uri("sourceUri", form.text("sourceUri"), false);
            byte[] content = VocabularyController.requireFile(form, checks);
            checks.throwIfAny();
            ExternalVocabulary created = service.register(
                    new ExternalVocabulary(
                            name,
                            namespace,
                            aliases,
                            source,
                            description,
                            user.creator(),
                            ExternalVocabularyRepository.now()),
                    content);
            Map<String, Object> body = representations.external(created);
            return ResponseEntity.created(URI.create((String) body.get("href")))
                    .header("Content-Type", JSON)
                    .body(body);
        }
    }

    @PostMapping(value = "/v1/external-vocabulary/searches", produces = JSON, consumes = JSON)
    public Map<String, Object> searchExternalVocabularies(HttpServletRequest request) {
        RequestChecks.start().noQuery(request.getQueryString()).throwIfAny();
        List<String> uris = SearchRequests.external(jsonBody.readObject(request));
        return representations.searchResult(
                service.search(uris).stream().map(representations::external).toList());
    }

    @PostMapping(value = "/v1/external-vocabularies/content", produces = ContentController.TURTLE, consumes = JSON)
    public ResponseEntity<byte[]> downloadExternalVocabularyContent(HttpServletRequest request) {
        RequestChecks.start().noQuery(request.getQueryString()).throwIfAny();
        String namespace = SearchRequests.contentNamespace(jsonBody.readObject(request));
        Map.Entry<String, byte[]> found = service.contentByNamespace(namespace);
        return ContentController.turtle(found.getValue(), found.getKey() + ".ttl");
    }

    @GetMapping(value = "/v1/external-vocabularies/{name}/content", produces = ContentController.TURTLE)
    public ResponseEntity<byte[]> getExternalVocabularyContentByName(
            @PathVariable("name") String name, HttpServletRequest request) {
        RequestChecks.start()
                .pathName("name", name)
                .noQuery(request.getQueryString())
                .throwIfAny();
        return ContentController.turtle(service.content(name), name + ".ttl");
    }

    @GetMapping(value = "/v1/external-vocabularies/{name}", produces = JSON)
    public Map<String, Object> getExternalVocabulary(@PathVariable("name") String name, HttpServletRequest request) {
        RequestChecks.start()
                .pathName("name", name)
                .noQuery(request.getQueryString())
                .throwIfAny();
        return representations.external(service.get(name));
    }

    @DeleteMapping(value = "/v1/external-vocabularies/{name}")
    public ResponseEntity<Void> deleteExternalVocabulary(
            @PathVariable("name") String name, HttpServletRequest request) {
        RequestChecks.start()
                .pathName("name", name)
                .noQuery(request.getQueryString())
                .throwIfAny();
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    /** At most 20 aliases, each absolute, none equal to the namespace or another alias (D56, D73). */
    static List<String> aliases(List<String> values, String namespace, RequestChecks checks) {
        List<String> aliases = new ArrayList<>();
        if (values.size() > MAX_ALIASES) {
            checks.add(Issue.schema("body", "aliasUris", "At most 20 aliasUris."));
            return aliases;
        }
        Set<String> seen = new HashSet<>();
        if (namespace != null) {
            seen.add(ExternalVocabularyRepository.normalise(namespace));
        }
        for (String value : values) {
            if (!eu.simpl.vocabularymanager.validation.UriRules.isAbsoluteUri(value)) {
                checks.add(Issue.schema("body", "aliasUris", "Each aliasUris value must be an absolute URI."));
                return aliases;
            }
            if (!seen.add(ExternalVocabularyRepository.normalise(value))) {
                checks.add(Issue.schema(
                        "body",
                        "aliasUris",
                        "An alias must differ from the namespace URI and from the other aliases."));
                return aliases;
            }
            aliases.add(value);
        }
        return aliases;
    }
}

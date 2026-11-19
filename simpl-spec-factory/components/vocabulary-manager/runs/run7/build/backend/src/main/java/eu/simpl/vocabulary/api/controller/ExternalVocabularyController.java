package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.ApiMapper;
import eu.simpl.vocabulary.api.dto.ExternalVocabularyDto;
import eu.simpl.vocabulary.api.dto.PageDto;
import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.api.request.MultipartForm;
import eu.simpl.vocabulary.api.request.QueryParameters;
import eu.simpl.vocabulary.api.request.QueryRules;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.security.Roles;
import eu.simpl.vocabulary.service.ExternalVocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** External vocabulary operations (D30, D41, D42, D56, D70, D73). */
@RestController
@RequestMapping("/v1/external-vocabularies")
public class ExternalVocabularyController {

    private static final Set<String> FIELDS =
            Set.of(MultipartForm.FILE_FIELD, "name", "namespaceUri", "aliasUris", "description", "sourceUri");

    private final ExternalVocabularyService service;
    private final ApiMapper mapper;
    private final AppProperties.Limits limits;

    public ExternalVocabularyController(ExternalVocabularyService service, ApiMapper mapper, AppProperties properties) {
        this.service = service;
        this.mapper = mapper;
        this.limits = properties.limits();
    }

    @GetMapping
    public ResponseEntity<PageDto<ExternalVocabularyDto>> list(HttpServletRequest request) {
        QueryParameters query = QueryParameters.parse(request.getQueryString());
        List<Issue> issues = new ArrayList<>(query.allowOnly(Set.of("page", "pageSize")));
        long page = QueryRules.page(query, issues);
        int pageSize = QueryRules.pageSize(query, issues);
        Requests.rejectIfAny(issues);
        return Requests.json(
                200, ApiMapper.page("/external-vocabularies", query, service.list(page, pageSize), mapper::external));
    }

    @PostMapping
    public ResponseEntity<ExternalVocabularyDto> register(HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        MultipartForm form = MultipartForm.read(request, limits.maxFileBytes(), limits.maxRequestBytes());
        issues.addAll(form.unknownOrRepeated(FIELDS, Set.of("aliasUris")));
        String name = Requests.text(form, "name");
        if (name == null) {
            issues.add(FieldRules.required("name"));
        } else {
            FieldRules.checkName(Issue.IN_BODY, "name", name).ifPresent(issues::add);
        }
        String namespaceUri = Requests.text(form, "namespaceUri");
        if (namespaceUri == null) {
            issues.add(FieldRules.required("namespaceUri"));
        } else if (!FieldRules.isAbsoluteUri(namespaceUri)) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "namespaceUri", "'namespaceUri' must be an absolute URI."));
        }
        List<String> aliases =
                form.all("aliasUris").stream().map(MultipartForm.FormPart::text).toList();
        aliasIssue(namespaceUri, aliases).ifPresent(issues::add);
        String sourceUri = Requests.text(form, "sourceUri");
        if (sourceUri != null && !FieldRules.isAbsoluteUri(sourceUri)) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "sourceUri", "'sourceUri' must be an absolute URI."));
        }
        String description = Requests.text(form, "description");
        FieldRules.checkOptionalText("description", description).ifPresent(issues::add);
        Optional<MultipartForm.FormPart> file = form.first(MultipartForm.FILE_FIELD);
        if (file.isEmpty()) {
            issues.add(FieldRules.required(MultipartForm.FILE_FIELD));
        }
        Requests.rejectIfAny(issues);
        String trimmedDescription = description == null || description.isBlank() ? null : description.strip();
        ExternalVocabulary registration = new ExternalVocabulary(
                name, namespaceUri, aliases, sourceUri, trimmedDescription, Roles.creator(), null);
        ExternalVocabulary stored = service.register(registration, file.get().bytes());
        ExternalVocabularyDto body = mapper.external(stored);
        return ResponseEntity.created(URI.create(body.href()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * Alias rules of D56 and D73: at most 20, absolute, none equal to the namespace or to another alias.
     *
     * @param namespaceUri the namespace URI, may be null
     * @param aliases the aliases as sent
     * @return an issue when a rule is broken
     */
    static Optional<Issue> aliasIssue(String namespaceUri, List<String> aliases) {
        if (aliases.size() > FieldRules.MAX_ALIASES) {
            return Optional.of(
                    Issue.schemaViolation(Issue.IN_BODY, "aliasUris", "At most 20 'aliasUris' may be given."));
        }
        Set<String> seen = new HashSet<>();
        if (namespaceUri != null) {
            seen.add(FieldRules.normaliseNamespace(namespaceUri));
        }
        for (String alias : aliases) {
            if (!FieldRules.isAbsoluteUri(alias)) {
                return Optional.of(
                        Issue.schemaViolation(Issue.IN_BODY, "aliasUris", "'aliasUris' must hold absolute URIs."));
            }
            if (!seen.add(FieldRules.normaliseNamespace(alias))) {
                return Optional.of(Issue.schemaViolation(
                        Issue.IN_BODY, "aliasUris", "An alias may not repeat the namespace URI or another alias."));
            }
        }
        return Optional.empty();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ExternalVocabularyDto> get(@PathVariable String name, HttpServletRequest request) {
        checkNameRequest(name, request);
        return Requests.json(200, mapper.external(service.get(name)));
    }

    @GetMapping(value = "/{name}/content", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> content(@PathVariable String name, HttpServletRequest request) {
        checkNameRequest(name, request);
        return Requests.turtle(service.content(name).bytes(), name + ".ttl");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name, HttpServletRequest request) {
        checkNameRequest(name, request);
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    private static void checkNameRequest(String name, HttpServletRequest request) {
        List<Issue> issues =
                new ArrayList<>(QueryParameters.parse(request.getQueryString()).requireNone());
        Requests.checkPathName("name", name, issues);
        Requests.rejectIfAny(issues);
    }
}

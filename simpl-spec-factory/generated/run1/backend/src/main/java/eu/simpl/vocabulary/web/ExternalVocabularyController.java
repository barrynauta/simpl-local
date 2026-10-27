package eu.simpl.vocabulary.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import eu.simpl.vocabulary.service.ExternalVocabularyService;
import eu.simpl.vocabulary.service.Upload;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/** Cached external vocabularies (OpenAPI only, no story). */
@RestController
@RequestMapping("/v1")
public class ExternalVocabularyController {

    private static final Set<String> LIST_PARAMS = Set.of("page", "pageSize");
    private static final Set<String> SEARCH_FIELDS = Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");

    private final ExternalVocabularyService service;
    private final ResourceMapper mapper;
    private final AppProperties props;

    public ExternalVocabularyController(ExternalVocabularyService service, ResourceMapper mapper,
                                        AppProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.props = props;
    }

    @GetMapping(path = "/external-vocabularies", produces = MediaType.APPLICATION_JSON_VALUE)
    public Dtos.PageResponse<Dtos.ExternalVocabularyDto> list(@RequestParam MultiValueMap<String, String> query) {
        List<ProblemIssue> issues = new ArrayList<>();
        RequestChecks.onlyAllowed(query, LIST_PARAMS, issues);
        RequestChecks.Paging paging = RequestChecks.paging(query, props.maxPageSize(), issues);
        RequestChecks.throwIfAny(issues);
        return RequestChecks.page(service.findAll(), paging, "/external-vocabularies", new LinkedHashMap<>(),
                mapper::external);
    }

    @PostMapping(path = "/external-vocabularies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Dtos.ExternalVocabularyDto> create(MultipartHttpServletRequest request, Authentication auth)
            throws IOException {
        List<ProblemIssue> issues = new ArrayList<>();
        String name = trimmed(request.getParameter("name"));
        if (name == null) {
            issues.add(ProblemIssue.schemaViolation("body", "name", null, "Field 'name' is required."));
        } else if (!RequestChecks.validName(name)) {
            issues.add(ProblemIssue.schemaViolation("body", "name", name, "Field 'name' " + RequestChecks.NAME_RULE
                    + "."));
        }
        String namespaceUri = trimmed(request.getParameter("namespaceUri"));
        if (namespaceUri == null) {
            issues.add(ProblemIssue.schemaViolation("body", "namespaceUri", null, "Field 'namespaceUri' is required."));
        } else if (!RequestChecks.isAbsoluteUri(namespaceUri)) {
            issues.add(ProblemIssue.schemaViolation("body", "namespaceUri", namespaceUri,
                    "Field 'namespaceUri' must be an absolute URI."));
        }
        List<String> aliases = new ArrayList<>();
        String[] rawAliases = request.getParameterValues("aliasUris");
        if (rawAliases != null) {
            for (String raw : rawAliases) {
                for (String a : raw.split(",")) {
                    String alias = a.trim();
                    if (alias.isEmpty()) {
                        continue;
                    }
                    if (!RequestChecks.isAbsoluteUri(alias)) {
                        issues.add(ProblemIssue.schemaViolation("body", "aliasUris", alias,
                                "Every value of 'aliasUris' must be an absolute URI."));
                    } else if (!aliases.contains(alias)) {
                        aliases.add(alias);
                    }
                }
            }
        }
        String sourceUri = trimmed(request.getParameter("sourceUri"));
        if (sourceUri != null && !RequestChecks.isAbsoluteUri(sourceUri)) {
            issues.add(ProblemIssue.schemaViolation("body", "sourceUri", sourceUri,
                    "Field 'sourceUri' must be an absolute URI."));
        }
        String description = trimmed(request.getParameter("description"));
        Upload file = VocabularyController.file(request, issues);
        RequestChecks.throwIfAny(issues);
        ExternalVocabulary created = service.create(name, namespaceUri, aliases, sourceUri, description, file,
                RequestChecks.user(auth));
        Dtos.ExternalVocabularyDto body = mapper.external(created);
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, body.href())
                .contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @PostMapping(path = "/external-vocabulary/searches", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Dtos.PageResponse<Dtos.ExternalVocabularyDto> search(@RequestBody JsonNode body) {
        List<ProblemIssue> issues = new ArrayList<>();
        if (body == null || !body.isObject()) {
            throw ApiException.badRequest(ProblemIssue.schemaViolation("body", null, null,
                    "The request body must be a JSON object."));
        }
        body.fieldNames().forEachRemaining(f -> {
            if (!SEARCH_FIELDS.contains(f)) {
                issues.add(ProblemIssue.schemaViolation("body", f, null, "Property '" + f + "' is not allowed."));
            }
        });
        Set<String> namespaces = new LinkedHashSet<>();
        Set<String> aliases = new LinkedHashSet<>();
        boolean single = body.has("namespaceUri") || body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int singles = (body.has("namespaceUri") ? 1 : 0) + (body.has("aliasUri") ? 1 : 0);
        if (singles > 1 || (single && batch)) {
            issues.add(ProblemIssue.schemaViolation("body", null, null,
                    "Submit exactly one of 'namespaceUri', 'aliasUri', or a batch with 'namespaceUris' and/or "
                            + "'aliasUris'."));
        } else if (!single && !batch) {
            issues.add(ProblemIssue.schemaViolation("body", null, null,
                    "One of 'namespaceUri', 'aliasUri', 'namespaceUris' or 'aliasUris' is required."));
        }
        readUri(body, "namespaceUri", namespaces, issues);
        readUri(body, "aliasUri", aliases, issues);
        readUris(body, "namespaceUris", namespaces, issues);
        readUris(body, "aliasUris", aliases, issues);
        if (batch && !single && namespaces.isEmpty() && aliases.isEmpty() && issues.isEmpty()) {
            issues.add(ProblemIssue.schemaViolation("body", null, null,
                    "A batch search needs at least one namespace URI or alias URI candidate."));
        }
        RequestChecks.throwIfAny(issues);
        List<Dtos.ExternalVocabularyDto> items = service.findAll().stream()
                .filter(x -> namespaces.stream().anyMatch(x::matchesNamespace)
                        || aliases.stream().anyMatch(x::matchesAlias))
                .map(mapper::external)
                .toList();
        return new Dtos.PageResponse<>(1, items.isEmpty() ? 0 : 1, items.size(), Math.max(20, items.size()), null,
                null, null, null, null, items);
    }

    @PostMapping(path = "/external-vocabularies/content", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "text/turtle")
    public ResponseEntity<byte[]> contentByNamespace(@RequestBody JsonNode body) {
        JsonNode ns = body == null ? null : body.get("namespaceUri");
        if (ns == null || !ns.isTextual() || !RequestChecks.isAbsoluteUri(ns.asText())) {
            throw ApiException.badRequest(ProblemIssue.schemaViolation("body", "namespaceUri",
                    ns == null ? null : ns.asText(), "Property 'namespaceUri' is required and must be an absolute URI."));
        }
        ExternalVocabulary x = service.byNamespaceOrAlias(ns.asText());
        return ContentController.turtle(service.content(x.name()), x.name() + ".ttl");
    }

    @GetMapping(path = "/external-vocabularies/{name}/content", produces = "text/turtle")
    public ResponseEntity<byte[]> contentByName(@PathVariable String name) {
        RequestChecks.requireName(name, "name");
        return ContentController.turtle(service.content(name), name + ".ttl");
    }

    @GetMapping(path = "/external-vocabularies/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Dtos.ExternalVocabularyDto get(@PathVariable String name) {
        RequestChecks.requireName(name, "name");
        return mapper.external(service.get(name));
    }

    @DeleteMapping(path = "/external-vocabularies/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        RequestChecks.requireName(name, "name");
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    private static void readUri(JsonNode body, String field, Set<String> out, List<ProblemIssue> issues) {
        JsonNode n = body.get(field);
        if (n == null) {
            return;
        }
        if (!n.isTextual() || !RequestChecks.isAbsoluteUri(n.asText())) {
            issues.add(ProblemIssue.schemaViolation("body", field, n.asText(),
                    "Property '" + field + "' must be an absolute URI."));
            return;
        }
        out.add(n.asText().trim());
    }

    private static void readUris(JsonNode body, String field, Set<String> out, List<ProblemIssue> issues) {
        JsonNode n = body.get(field);
        if (n == null) {
            return;
        }
        if (!n.isArray()) {
            issues.add(ProblemIssue.schemaViolation("body", field, null, "Property '" + field + "' must be an array."));
            return;
        }
        for (JsonNode e : n) {
            if (!e.isTextual() || !RequestChecks.isAbsoluteUri(e.asText())) {
                issues.add(ProblemIssue.schemaViolation("body", field, e.asText(),
                        "Every value of '" + field + "' must be an absolute URI."));
            } else {
                out.add(e.asText().trim());
            }
        }
    }

    private static String trimmed(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

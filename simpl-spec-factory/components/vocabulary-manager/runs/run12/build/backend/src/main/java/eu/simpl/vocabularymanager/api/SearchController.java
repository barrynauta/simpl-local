package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.SearchService;
import eu.simpl.vocabularymanager.domain.SearchService.Reference;
import eu.simpl.vocabularymanager.error.Issue;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** POST /vocabularies/searches (D38, D54, D68, D153). */
@RestController
public class SearchController {

    private static final int MAX_ITEMS = 100;

    private final SearchService service;
    private final JsonBody json;
    private final String contentBase;

    public SearchController(SearchService service, JsonBody json, AppProperties props) {
        this.service = service;
        this.json = json;
        this.contentBase = props.contentBaseUrl();
    }

    @PostMapping(
            path = "/v1/vocabularies/searches",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Dto.PageDto<Dto.SearchItemDto> search(HttpServletRequest request) {
        RequestRules.noQuery(request.getQueryString());
        List<Reference> refs = references(json.read(request));
        List<Dto.SearchItemDto> items = new ArrayList<>();
        for (SearchService.Result r : service.search(refs)) {
            String href =
                    r.latest() ? "/vocabularies/" + r.name() : "/vocabularies/" + r.name() + "/versions/" + r.version();
            String contentHref = r.latest()
                    ? contentBase + "/content/" + r.name()
                    : contentBase + "/content/" + r.name() + "/versions/" + r.version();
            items.add(new Dto.SearchItemDto(
                    href, contentHref, r.name(), r.version(), r.status().name(), r.latest()));
        }
        return Dto.PageDto.search(items);
    }

    /** VocabularySearchRequest: items 1 to 100, each name and optional version (D38, D68). */
    static List<Reference> references(JsonNode body) {
        List<Issue> issues = RequestRules.issues();
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation("body", "body", "The body must be a JSON object.", null));
            RequestRules.throwIfAny(issues);
        }
        RequestRules.unknownProperties(body, Set.of("items"), "", issues);
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > MAX_ITEMS) {
            issues.add(
                    Issue.schemaViolation("body", "items", "'items' must be an array of 1 to 100 references.", null));
            RequestRules.throwIfAny(issues);
        }
        List<Reference> refs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String prefix = "items[" + i + "]";
            if (!item.isObject()) {
                issues.add(Issue.schemaViolation("body", prefix, "Each item must be a JSON object.", null));
                continue;
            }
            RequestRules.unknownProperties(item, Set.of("name", "version"), prefix, issues);
            JsonNode n = item.get("name");
            String name = null;
            if (n == null || !n.isString() || !Validation.validName(n.asString())) {
                issues.add(Issue.schemaViolation(
                        "body",
                        prefix + ".name",
                        "'name' must be 3 to 64 characters matching ^[A-Z][a-zA-Z0-9]*$.",
                        null));
            } else {
                name = n.asString();
            }
            JsonNode v = item.get("version");
            Integer version = null;
            if (v != null) {
                if (v.isIntegralNumber() && v.canConvertToInt() && v.asInt() >= 1) {
                    version = v.asInt();
                } else {
                    issues.add(Issue.schemaViolation(
                            "body", prefix + ".version", "'version' must be an integer from 1 to 2147483647.", null));
                }
            }
            if (name != null) {
                refs.add(new Reference(name, version));
            }
        }
        RequestRules.throwIfAny(issues);
        return refs;
    }
}

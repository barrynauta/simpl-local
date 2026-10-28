package eu.simpl.catalogue.cat.api;

import eu.simpl.catalogue.cat.service.SearchService;
import eu.simpl.catalogue.common.AdvancedQuery;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.common.Roles;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Local quick and advanced search of catalogue-be, called by qma only (D4). */
@RestController
public class SearchController {
    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    /** D69: the User-Attributes header, when present, is the attribute set; else the token attributes. */
    static List<String> attributes(Caller caller, String header) {
        return header != null ? FederationConfig.splitAttributes(header) : caller.identityAttributes();
    }

    @GetMapping(value = "/selfDescriptions/quickSearch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> quick(@RequestParam(required = false) String searchTerm,
                                        @RequestParam(required = false) String minimumScore,
                                        @RequestParam(required = false) String minimumClassification,
                                        @RequestParam(required = false) String page,
                                        @RequestParam(required = false) String pageSize,
                                        @RequestHeader(value = "User-Attributes", required = false) String attrs) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        if (searchTerm == null) throw ApiException.invalidInput("Required query parameter 'searchTerm' is missing.");
        if (searchTerm.isBlank()) throw ApiException.invalidInput("searchTerm must not be blank.");
        QualityParams q = QualityParams.parse(minimumScore, minimumClassification, null);
        Paging paging = Paging.parse(page, pageSize);
        return SelfDescriptionController.json(200, search.quick(searchTerm, attributes(caller, attrs), q, paging));
    }

    @PostMapping(value = "/selfDescriptions/advancedSearch", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> advanced(@RequestBody(required = false) String body,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize,
                                           @RequestHeader(value = "User-Attributes", required = false) String attrs) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        Paging paging = Paging.parse(page, pageSize);
        JsonNode root = Json.parseOrNull(body);
        if (root != null && root.isTextual()) root = Json.parseOrNull(root.asText());
        if (root == null || !root.isObject()) throw ApiException.badRequest("The request body must be a JSON object.");
        AdvancedQuery query = AdvancedQuery.parse(root);
        QualityParams q = QualityParams.fromBody(root, null);
        return SelfDescriptionController.json(200, search.advanced(query, attributes(caller, attrs), q, paging));
    }
}

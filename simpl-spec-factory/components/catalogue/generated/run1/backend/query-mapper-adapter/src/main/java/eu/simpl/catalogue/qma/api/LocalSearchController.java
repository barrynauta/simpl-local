package eu.simpl.catalogue.qma.api;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.AdvancedQuery;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.common.Roles;
import eu.simpl.catalogue.qma.client.CatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Local quick and advanced search of the own catalogue (SIMPL-31111, SIMPL-31112, D32, D69). */
@RestController
@RequestMapping("/v1")
public class LocalSearchController {
    private final CatClient cat;
    private final FederationConfig cfg;

    public LocalSearchController(CatClient cat, FederationConfig cfg) {
        this.cat = cat;
        this.cfg = cfg;
    }

    @GetMapping(value = "/selfDescriptions", produces = "application/json")
    public ResponseEntity<String> quick(@RequestParam(required = false) String searchString,
                                        @RequestParam(required = false) String minimumScore,
                                        @RequestParam(required = false) String minimumClassification,
                                        @RequestParam(required = false) String page,
                                        @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        String s = Support.searchString(searchString);
        QualityParams q = QualityParams.parse(minimumScore, minimumClassification, null);
        Paging p = Paging.parse(page, pageSize);
        JsonNode res = cat.quick(caller.bearer(), caller.attributesHeader(), s, q, p);
        return Support.json(Support.fromCat(res, p, Support.qualityQuery(Http.query().add("searchString", s), q),
                "/selfDescriptions", false));
    }

    @PostMapping(value = "/selfDescriptions/advancedSearch", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> advanced(@RequestBody(required = false) String text,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        Paging p = Paging.parse(page, pageSize);
        JsonNode body = Support.body(text);
        List<String> ids = Support.catalogueIdsFromBody(body, true);
        AdvancedQuery.parse(body);
        QualityParams.fromBody(body, null);
        if (ids.stream().noneMatch(cfg::isOwn)) {
            return Support.json(Support.page(0, List.of(), p, Http.query(), "/selfDescriptions/advancedSearch", false));
        }
        JsonNode res = cat.advanced(caller.bearer(), caller.attributesHeader(), AdvancedQuery.withoutReserved(body, true), p);
        return Support.json(Support.fromCat(res, p, Http.query(), "/selfDescriptions/advancedSearch", false));
    }
}

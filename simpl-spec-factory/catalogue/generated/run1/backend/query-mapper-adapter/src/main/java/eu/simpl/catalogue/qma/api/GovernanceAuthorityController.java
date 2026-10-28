package eu.simpl.catalogue.qma.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.AccessPolicy;
import eu.simpl.catalogue.common.AdvancedQuery;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.common.Roles;
import eu.simpl.catalogue.qma.client.CatClient;
import eu.simpl.catalogue.qma.client.FederationClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Trusted GA entry point: search across selected catalogues and detail lookup (SIMPL-31113, SIMPL-31153, D6, D7,
 * D38 to D44).
 */
@RestController
@RequestMapping("/v1/governanceAuthority")
public class GovernanceAuthorityController {
    private final CatClient cat;
    private final FederationClient federation;
    private final FederationConfig cfg;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public GovernanceAuthorityController(CatClient cat, FederationClient federation, FederationConfig cfg) {
        this.cat = cat;
        this.federation = federation;
        this.cfg = cfg;
    }

    @GetMapping(value = "/selfDescriptions", produces = "application/json")
    public ResponseEntity<String> quick(HttpServletRequest req,
                                        @RequestParam(required = false) String searchString,
                                        @RequestParam(required = false) String minimumScore,
                                        @RequestParam(required = false) String minimumClassification,
                                        @RequestParam(required = false) String page,
                                        @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        List<String> ids = Support.catalogueIds(req.getParameterValues("catalogueIds"));
        Support.checkKnown(cfg, ids);
        String s = Support.searchString(searchString);
        QualityParams q = QualityParams.parse(minimumScore, minimumClassification, null);
        Paging p = Paging.parse(page, pageSize);
        Http.Query fedQuery = Http.query().add("searchString", s).add("page", p.page()).add("pageSize", p.pageSize());
        List<String> resolved = resolve(ids);
        List<Supplier<JsonNode>> calls = new ArrayList<>();
        for (String id : resolved) {
            if (cfg.isOwn(id)) {
                calls.add(() -> cat.quick(caller.bearer(), caller.attributesHeader(), s, q, p));
            } else {
                FederationConfig.FederatedCatalogue c = cfg.federated(id).orElseThrow();
                Http.Query fq = Http.query().add("catalogueIds", id).add("searchString", s)
                        .add("page", p.page()).add("pageSize", p.pageSize());
                calls.add(() -> federation.call(c, HttpMethod.GET, fq.relative("/federated/selfDescriptions"),
                        null, caller.attributesHeader(), false));
            }
        }
        Http.Query next = Http.query().addAll("catalogueIds", ids).add("searchString", s);
        Support.qualityQuery(next, q);
        return combine(resolved, calls, p, next, "/governanceAuthority/selfDescriptions");
    }

    /** D6: tier-1 draft, operationId advancedSearchGovernanceAuthoritySelfDescriptions. */
    @PostMapping(value = "/selfDescriptions/advancedSearch", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> advanced(@RequestBody(required = false) String text,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        JsonNode body = Support.body(text);
        List<String> ids = Support.catalogueIds(Support.catalogueIdsFromBody(body, true));
        Support.checkKnown(cfg, ids);
        AdvancedQuery.parse(body);
        QualityParams.fromBody(body, null);
        Paging p = Paging.parse(page, pageSize);
        List<String> resolved = resolve(ids);
        List<Supplier<JsonNode>> calls = new ArrayList<>();
        for (String id : resolved) {
            if (cfg.isOwn(id)) {
                calls.add(() -> cat.advanced(caller.bearer(), caller.attributesHeader(),
                        AdvancedQuery.withoutReserved(body, true), p));
            } else {
                FederationConfig.FederatedCatalogue c = cfg.federated(id).orElseThrow();
                ObjectNode fb = AdvancedQuery.withoutReserved(body, false);
                fb.putArray("catalogueIds").add(id);
                String path = Http.query().add("page", p.page()).add("pageSize", p.pageSize())
                        .relative("/federated/selfDescriptions/advancedSearch");
                calls.add(() -> federation.call(c, HttpMethod.POST, path, Json.write(fb), caller.attributesHeader(), false));
            }
        }
        return combine(resolved, calls, p, Http.query(), "/governanceAuthority/selfDescriptions/advancedSearch");
    }

    /** Own ids collapse to the own catalogue id; order is kept. */
    private List<String> resolve(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            String r = cfg.isOwn(id) ? cfg.ownCatalogue.id : id;
            if (!out.contains(r)) out.add(r);
        }
        return out;
    }

    /** D39: one id fails with its error; several ids concatenate the reachable pages and name the failed ones. */
    private ResponseEntity<String> combine(List<String> ids, List<Supplier<JsonNode>> calls, Paging p,
                                           Http.Query next, String path) {
        List<CompletableFuture<JsonNode>> futures = calls.stream()
                .map(c -> CompletableFuture.supplyAsync(c, pool)).toList();
        List<JsonNode> items = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        long total = 0;
        int maxPages = 0;
        for (int i = 0; i < futures.size(); i++) {
            String id = ids.get(i);
            JsonNode res;
            try {
                res = futures.get(i).join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ApiException ae) {
                    boolean federationError = ae.status() == 502 || ae.status() == 504;
                    if (ids.size() == 1 || !federationError || cfg.isOwn(id)) throw ae;
                    failed.add(id);
                    continue;
                }
                throw new ApiException(ProblemTypes.INTERNAL, "The search failed.");
            }
            boolean own = cfg.isOwn(id);
            for (JsonNode item : res.path("items")) {
                JsonNode copy = item.deepCopy();
                if (!own && copy.path("n").isObject()) {
                    ((ObjectNode) copy.get("n")).putNull("overallScore");
                    ((ObjectNode) copy.get("n")).putNull("overallClassification");
                }
                items.add(copy);
            }
            long t = res.has("total") ? res.path("total").asLong() : res.path("totalCount").asLong();
            total += t;
            maxPages = Math.max(maxPages, p.totalPages(t));
        }
        ObjectNode out = Support.page(total, items, p, Http.query(), path, false);
        out.put("totalPages", maxPages);
        if (p.page() < maxPages) {
            next.add("page", p.page() + 1).add("pageSize", p.pageSize());
            out.put("next", next.relative(path));
        } else {
            out.putNull("next");
        }
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().header("Content-Type", "application/json");
        if (!failed.isEmpty()) b.header("Simpl-Failed-Catalogues", String.join(",", failed));
        return b.body(Json.write(out));
    }

    /** D7, section 6.1: detail through the Consumer's GA. */
    @GetMapping(value = "/resourceDescriptions/{resourceDescriptionId}", produces = "application/json")
    public ResponseEntity<String> detail(@PathVariable String resourceDescriptionId,
                                         @RequestParam(required = false) String catalogueId) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        if (catalogueId == null || !FederationConfig.CATALOGUE_ID.matcher(catalogueId).matches()) {
            throw ApiException.invalidInput("catalogueId is missing or malformed.");
        }
        if (!cfg.isOwn(catalogueId) && cfg.federated(catalogueId).isEmpty()) {
            throw ApiException.invalidInput("Unknown catalogue id '" + catalogueId + "'.");
        }
        ObjectNode out = Json.object();
        if (cfg.isOwn(catalogueId)) {
            JsonNode item = cat.find(caller.bearer(), resourceDescriptionId)
                    .filter(i -> "active".equals(i.path("meta").path("status").asText()))
                    .orElseThrow(() -> new ApiException(ProblemTypes.NOT_FOUND,
                            "Resource description " + resourceDescriptionId + " was not found."));
            JsonNode vc = Json.parse(item.path("content").asText());
            if (!AccessPolicy.discoverable(vc, caller.identityAttributes())) {
                throw new ApiException(ProblemTypes.DISCOVERY_POLICY_DENIED,
                        "The discovery access policy of the resource description does not permit this Consumer.");
            }
            out.put("catalogueId", cfg.ownCatalogue.id);
            out.set("resourceDescription", vc);
            JsonNode qa = item.path("meta").get("qualityAssessment");
            if (qa != null && !qa.isNull()) out.set("qualityAssessment", qa);
        } else {
            FederationConfig.FederatedCatalogue c = cfg.federated(catalogueId).orElseThrow();
            JsonNode res = federation.call(c, HttpMethod.GET, "/federated/resourceDescriptions/" + Http.seg(resourceDescriptionId),
                    null, caller.attributesHeader(), true);
            JsonNode rd = res.get("resourceDescription");
            if (rd == null || !rd.isObject()) {
                throw new ApiException(ProblemTypes.INTERNAL, "The Trustor answer has no resource description.");
            }
            out.put("catalogueId", c.id);
            out.set("resourceDescription", rd);
        }
        return Support.json(out);
    }
}

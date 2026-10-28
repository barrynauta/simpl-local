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
import eu.simpl.catalogue.common.TokenClient;
import eu.simpl.catalogue.qma.client.CatClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trustor side: inbound federated search and detail (SIMPL-31138, SIMPL-31139, SIMPL-31150, D36, D37, D44).
 * The calling GA is authenticated by its federation token; its trust is checked against trustedAuthorities.
 */
@RestController
@RequestMapping("/v1/federated")
public class FederatedController {
    private final CatClient cat;
    private final FederationConfig cfg;
    private final TokenClient service;

    public FederatedController(CatClient cat, FederationConfig cfg) {
        this.cat = cat;
        this.cfg = cfg;
        this.service = cfg.serviceClient == null ? null : new TokenClient(cfg.serviceClient);
    }

    private record Inbound(List<String> attributes) {}

    private Inbound inbound(String userAttributes, String correlationId) {
        Caller caller = Caller.current();
        FederationConfig.TrustedAuthority ta = cfg.trusted(caller.issuer(), caller.azp())
                .orElseThrow(() -> ApiException.forbidden("The calling Governance Authority is not trusted."));
        if (userAttributes == null) throw ApiException.badRequest("Required forwarded header User-Attributes is missing.");
        if (correlationId == null || correlationId.isBlank()) {
            throw ApiException.badRequest("Required forwarded header Correlation-Id is missing.");
        }
        return new Inbound(FederationConfig.mapAttributes(ta, FederationConfig.splitAttributes(userAttributes)));
    }

    private String serviceBearer() {
        if (service == null) throw new ApiException(ProblemTypes.INTERNAL, "No service client is configured.");
        try {
            return "Bearer " + service.token();
        } catch (RuntimeException e) {
            throw new ApiException(ProblemTypes.INTERNAL, "The catalogue service token could not be obtained.");
        }
    }

    @GetMapping(value = "/selfDescriptions", produces = "application/json")
    public ResponseEntity<String> quick(HttpServletRequest req,
                                        @RequestParam(required = false) String searchString,
                                        @RequestParam(required = false) String page,
                                        @RequestParam(required = false) String pageSize,
                                        @RequestHeader(value = "User-Attributes", required = false) String attrs,
                                        @RequestHeader(value = "Correlation-Id", required = false) String cid) {
        Inbound in = inbound(attrs, cid);
        List<String> ids = Support.catalogueIds(req.getParameterValues("catalogueIds"));
        String s = Support.searchString(searchString);
        Paging p = Paging.parse(page, pageSize);
        Http.Query next = Http.query().addAll("catalogueIds", ids).add("searchString", s);
        if (ids.stream().noneMatch(id -> id.equals(cfg.ownCatalogue.id))) {
            return Support.json(Support.page(0, List.of(), p, next, "/federated/selfDescriptions", true));
        }
        JsonNode res = cat.quick(serviceBearer(), String.join(",", in.attributes()), s, new QualityParams(null, null), p);
        return Support.json(Support.fromCat(res, p, next, "/federated/selfDescriptions", true));
    }

    @PostMapping(value = "/selfDescriptions/advancedSearch", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> advanced(@RequestBody(required = false) String text,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize,
                                           @RequestHeader(value = "User-Attributes", required = false) String attrs,
                                           @RequestHeader(value = "Correlation-Id", required = false) String cid) {
        Inbound in = inbound(attrs, cid);
        JsonNode body = Support.body(text);
        List<String> ids = Support.catalogueIds(Support.catalogueIdsFromBody(body, true));
        AdvancedQuery.parse(body);
        Paging p = Paging.parse(page, pageSize);
        String path = "/federated/selfDescriptions/advancedSearch";
        if (ids.stream().noneMatch(id -> id.equals(cfg.ownCatalogue.id))) {
            return Support.json(Support.page(0, List.of(), p, Http.query(), path, true));
        }
        ObjectNode criteria = AdvancedQuery.withoutReserved(body, false);
        JsonNode res = cat.advanced(serviceBearer(), String.join(",", in.attributes()), criteria, p);
        return Support.json(Support.fromCat(res, p, Http.query(), path, true));
    }

    @GetMapping(value = "/resourceDescriptions/{resourceDescriptionId}", produces = "application/json")
    public ResponseEntity<String> detail(@PathVariable String resourceDescriptionId,
                                         @RequestHeader(value = "User-Attributes", required = false) String attrs,
                                         @RequestHeader(value = "Correlation-Id", required = false) String cid) {
        Inbound in = inbound(attrs, cid);
        JsonNode item = cat.find(serviceBearer(), resourceDescriptionId)
                .filter(i -> "active".equals(i.path("meta").path("status").asText()))
                .orElseThrow(() -> new ApiException(ProblemTypes.NOT_FOUND,
                        "Resource description with the requested identifier was not found."));
        JsonNode vc = Json.parse(item.path("content").asText());
        if (!AccessPolicy.discoverable(vc, in.attributes())) {
            throw new ApiException(ProblemTypes.DISCOVERY_POLICY_DENIED,
                    "The discovery access policy of the resource description does not permit this Consumer.");
        }
        ObjectNode out = Json.object();
        out.set("resourceDescription", vc);
        return Support.json(out);
    }
}

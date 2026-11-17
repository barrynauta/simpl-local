package eu.simpl.cm.manager.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.JsonBody;
import eu.simpl.cm.manager.service.EstablishmentService;
import eu.simpl.cm.manager.service.LifecycleService;
import eu.simpl.cm.manager.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** All CM operations of the OpenAPI and Appendix A, under /contract/v1. */
@RestController
public class CmController {
    private static final String JSON = "application/json";
    private static final String CT = "Content-Type";
    private final EstablishmentService establishment;
    private final LifecycleService lifecycle;
    private final QueryService query;
    private final ObjectMapper mapper;

    public CmController(EstablishmentService establishment, LifecycleService lifecycle, QueryService query, ObjectMapper mapper) {
        this.establishment = establishment;
        this.lifecycle = lifecycle;
        this.query = query;
        this.mapper = mapper;
    }

    private JsonNode object(byte[] body, String ct, boolean required) {
        return JsonBody.readObject(mapper, body, ct, required);
    }

    // ---- OpenAPI: verifiable-credentials
    @PostMapping(value = "/credentials/agreements/{aid}/definitions/{did}/issue", produces = JSON)
    public ResponseEntity<Map<String, Object>> issue(@PathVariable String aid, @PathVariable String did,
                                                     @RequestBody(required = false) byte[] body,
                                                     @RequestHeader(value = CT, required = false) String ct) {
        return ResponseEntity.status(201).body(establishment.issue(aid, did, object(body, ct, false)));
    }

    @PostMapping(value = "/credentials/agreements/{aid}/definitions/{did}/create", produces = JSON)
    public ResponseEntity<Map<String, Object>> create(@PathVariable String aid, @PathVariable String did,
                                                      @RequestBody(required = false) byte[] body,
                                                      @RequestHeader(value = CT, required = false) String ct) {
        return ResponseEntity.status(201).body(establishment.create(aid, did, object(body, ct, true)));
    }

    @PutMapping(value = "/verifications/{nid}", produces = JSON)
    public Map<String, Object> verify(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                      @RequestHeader(value = CT, required = false) String ct) {
        Ids.pathId("contractNegotiationId", nid);
        return establishment.verify(nid, JsonBody.read(mapper, body, ct, true));
    }

    @GetMapping(value = "/verifications/{nid}/vc", produces = JSON)
    public JsonNode ownVc(@PathVariable String nid) {
        return establishment.ownVc(nid);
    }

    // ---- OpenAPI: contract-agreements
    @PatchMapping(value = "/agreements/{aid}/definitions/{did}/status", produces = JSON)
    public Map<String, Object> agreementStatus(@PathVariable String aid, @PathVariable String did,
                                               @RequestBody(required = false) byte[] body,
                                               @RequestHeader(value = CT, required = false) String ct) {
        Ids.pathUuid("contractAgreementId", aid);
        return establishment.agreementStatus(aid, did, object(body, ct, true));
    }

    @GetMapping(value = "/agreements/{aid}", produces = JSON)
    public Map<String, Object> agreement(@PathVariable String aid) {
        return query.agreement(aid);
    }

    @GetMapping(value = "/agreements/{aid}/file", produces = "text/plain")
    public ResponseEntity<String> agreementFile(@PathVariable String aid) {
        return ResponseEntity.ok().header(CT, "text/plain;charset=UTF-8").body(query.agreementFile(aid));
    }

    @PostMapping(value = "/agreements/{nid}/search", produces = JSON)
    public Map<String, Object> search(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                      @RequestHeader(value = CT, required = false) String ct) {
        if ("search".equals(nid)) {
            return searchEstablished(body, ct);
        }
        Ids.pathId("contractNegotiationId", nid);
        return establishment.search(nid, object(body, ct, true));
    }

    private Map<String, Object> searchEstablished(byte[] body, String ct) {
        JsonNode b = object(body, ct, true);
        return query.searchEstablished(JsonBody.optionalId(b, "contractDefinitionId"), JsonBody.optionalId(b, "assetId"),
                JsonBody.optionalId(b, "providerId"), JsonBody.optionalId(b, "consumerId"));
    }

    @PostMapping(value = "/agreements/search", produces = JSON)
    public Map<String, Object> searchAgreements(@RequestBody(required = false) byte[] body,
                                                @RequestHeader(value = CT, required = false) String ct) {
        return searchEstablished(body, ct);
    }

    // ---- OpenAPI: contract-ui
    @PostMapping(value = "/contractOffers/{nid}", produces = JSON)
    public Map<String, Object> transferOffer(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                             @RequestHeader(value = CT, required = false) String ct) {
        Ids.pathId("contractNegotiationId", nid);
        return establishment.transferOffer(nid, object(body, ct, true));
    }

    @PatchMapping(value = "/contractOffers/{nid}/{operation}", produces = JSON)
    public Map<String, Object> offerDecision(@PathVariable String nid, @PathVariable String operation,
                                             @RequestParam(required = false) String reason,
                                             @RequestHeader(value = "X-Actor", required = false) String actor) {
        return establishment.offerDecision(nid, operation, reason, actor);
    }

    @PutMapping(value = "/contractOffers/{nid}/update", produces = JSON)
    public Map<String, Object> updateOffer(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                           @RequestHeader(value = CT, required = false) String ct) {
        Ids.pathId("contractNegotiationId", nid);
        return establishment.updateOffer(nid, object(body, ct, true));
    }

    // ---- Appendix A
    @GetMapping(value = "/contracts", produces = JSON)
    public Map<String, Object> contracts(@RequestParam(required = false) String contractNegotiationId,
                                         @RequestParam(required = false) String resourceName,
                                         @RequestParam(required = false) String providerName,
                                         @RequestParam(required = false) String consumerName,
                                         @RequestParam(required = false) String status) {
        return query.list(contractNegotiationId, resourceName, providerName, consumerName, status);
    }

    @GetMapping(value = "/contracts/{nid}", produces = JSON)
    public Map<String, Object> contract(@PathVariable String nid) {
        return query.details(nid);
    }

    @PostMapping(value = "/contracts/{nid}/lifecycleActions", produces = JSON)
    public Map<String, Object> lifecycleAction(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                               @RequestHeader(value = CT, required = false) String ct) {
        lifecycle.act(nid, object(body, ct, true));
        return query.details(nid);
    }

    @PutMapping(value = "/contracts/{nid}/consentStatus", produces = JSON)
    public Map<String, Object> consent(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                       @RequestHeader(value = CT, required = false) String ct) {
        lifecycle.consent(nid, object(body, ct, true));
        return query.details(nid);
    }

    @GetMapping(value = "/audit/contractStatusCounts", produces = JSON)
    public Map<String, Object> counts() {
        return query.counts();
    }
}

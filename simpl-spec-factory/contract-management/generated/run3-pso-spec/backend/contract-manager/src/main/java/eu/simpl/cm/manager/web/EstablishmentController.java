package eu.simpl.cm.manager.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.Ids;
import eu.simpl.cm.manager.service.EstablishmentService;
import org.springframework.http.MediaType;
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

/**
 * The 11 business operations of the CM OpenAPI. Each handler follows the order of D84:
 * path formats here, body syntax by Spring, then record, fields, party and state in the service.
 */
@RestController
public class EstablishmentController {

    private final EstablishmentService service;

    public EstablishmentController(EstablishmentService service) {
        this.service = service;
    }

    @PostMapping(value = "/agreements/{nid}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> search(@PathVariable("nid") String nid,
                                      @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.search(nid, body);
    }

    @PostMapping(value = "/agreements/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> searchAgreements(@RequestBody(required = false) JsonNode body) {
        return service.searchAgreements(body);
    }

    @GetMapping(value = "/agreements/{aid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode agreement(@PathVariable("aid") String aid) throws Exception {
        if ("search".equals(aid)) {
            // /agreements/search is its own resource (POST only): a GET there is a wrong method, not a bad id.
            throw new org.springframework.web.HttpRequestMethodNotSupportedException("GET", java.util.List.of("POST"));
        }
        Ids.requirePathUuid("contractAgreementId", aid);
        return service.agreement(aid);
    }

    @GetMapping(value = "/agreements/{aid}/file", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> agreementFile(@PathVariable("aid") String aid) {
        Ids.requirePathUuid("contractAgreementId", aid);
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(service.agreementFile(aid));
    }

    @PatchMapping(value = "/agreements/{aid}/definitions/{did}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> agreementStatus(@PathVariable("aid") String aid, @PathVariable("did") String did,
                                               @RequestBody(required = false) JsonNode body) {
        Ids.requirePathUuid("contractAgreementId", aid);
        Ids.requirePathId("contractDefinitionId", did);
        return service.agreementStatus(aid, did, body);
    }

    @PostMapping(value = "/credentials/agreements/{aid}/definitions/{did}/issue",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> issue(@PathVariable("aid") String aid, @PathVariable("did") String did,
                                                     @RequestBody(required = false) JsonNode body) {
        Ids.requirePathUuid("contractAgreementId", aid);
        Ids.requirePathId("contractDefinitionId", did);
        return ResponseEntity.status(201).body(service.issue(aid, did, body));
    }

    @PostMapping(value = "/credentials/agreements/{aid}/definitions/{did}/create",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> create(@PathVariable("aid") String aid, @PathVariable("did") String did,
                                                      @RequestBody(required = false) JsonNode body) {
        Ids.requirePathUuid("contractAgreementId", aid);
        Ids.requirePathId("contractDefinitionId", did);
        return ResponseEntity.status(201).body(service.create(aid, did, body));
    }

    @PutMapping(value = "/verifications/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> verify(@PathVariable("nid") String nid, @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.verify(nid, body);
    }

    @GetMapping(value = "/verifications/{nid}/vc", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode ownVc(@PathVariable("nid") String nid) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.ownCredential(nid);
    }

    @PostMapping(value = "/contractOffers/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> replaceOffer(@PathVariable("nid") String nid,
                                            @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.replaceOffer(nid, body);
    }

    @PatchMapping(value = "/contractOffers/{nid}/{operation}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> decision(@PathVariable("nid") String nid, @PathVariable("operation") String operation,
                                        @RequestParam(value = "reason", required = false) String reason,
                                        @RequestHeader(value = "X-Actor", required = false) String actor) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.offerDecision(nid, operation, reason, actor);
    }

    @PutMapping(value = "/contractOffers/{nid}/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> offerUpdate(@PathVariable("nid") String nid,
                                           @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return service.offerStatusUpdate(nid, body);
    }
}

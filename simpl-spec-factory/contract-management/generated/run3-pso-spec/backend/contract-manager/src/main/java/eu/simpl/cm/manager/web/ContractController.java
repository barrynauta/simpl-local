package eu.simpl.cm.manager.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.Ids;
import eu.simpl.cm.manager.service.ClauseService;
import eu.simpl.cm.manager.service.LifecycleService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Operations added by the digest (Appendix A). */
@RestController
public class ContractController {

    private final LifecycleService lifecycle;
    private final ClauseService clauses;

    public ContractController(LifecycleService lifecycle, ClauseService clauses) {
        this.lifecycle = lifecycle;
        this.clauses = clauses;
    }

    @GetMapping(value = "/contracts", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> list(@RequestParam(value = "contractNegotiationId", required = false) String nid,
                                    @RequestParam(value = "resourceName", required = false) String resourceName,
                                    @RequestParam(value = "providerName", required = false) String providerName,
                                    @RequestParam(value = "consumerName", required = false) String consumerName,
                                    @RequestParam(value = "status", required = false) String status) {
        return lifecycle.list(nid, resourceName, providerName, consumerName, status);
    }

    @GetMapping(value = "/contracts/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> details(@PathVariable("nid") String nid) {
        Ids.requirePathId("contractNegotiationId", nid);
        return lifecycle.details(nid);
    }

    @PostMapping(value = "/contracts/{nid}/lifecycleActions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> act(@PathVariable("nid") String nid, @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return lifecycle.act(nid, body);
    }

    @PutMapping(value = "/contracts/{nid}/consentStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> consent(@PathVariable("nid") String nid, @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        return lifecycle.consent(nid, body);
    }

    @GetMapping(value = "/audit/contractStatusCounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> counts() {
        return lifecycle.counts();
    }

    @PostMapping(value = "/contractClauseDocuments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart(value = "file", required = false)
                                                      MultipartFile file) {
        return ResponseEntity.status(201).body(clauses.upload(file));
    }
}

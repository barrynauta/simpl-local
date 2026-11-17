package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Connector endpoints called by CM (C.1). */
@RestController
public class EdcController {

    private final String apiKey;
    private final StubState state;
    private final Choreography choreography;
    private final Participants participants;

    public EdcController(@Value("${stub.edc-api-key}") String apiKey, StubState state, Choreography choreography,
                         Participants participants) {
        this.apiKey = apiKey;
        this.state = state;
        this.choreography = choreography;
        this.participants = participants;
    }

    private ResponseEntity<Object> unauthorized() {
        return ResponseEntity.status(401).header("Content-Type", "application/problem+json")
                .body(Map.of("type", "urn:problem-type:simpl:unauthorized", "title", "Unauthorized",
                        "status", 401, "detail", "Invalid API Key"));
    }

    private boolean authorized(String key) {
        return apiKey.equals(key);
    }

    @PostMapping("/edc/{p}/isPresent/{nid}/{value}")
    public ResponseEntity<Object> isPresent(@PathVariable String p, @PathVariable String nid,
                                            @PathVariable String value,
                                            @RequestHeader(value = "X-Api-Key", required = false) String key) {
        return callback(p, "isPresent", nid, value, key);
    }

    @PostMapping("/edc/{p}/isValid/{nid}/{value}")
    public ResponseEntity<Object> isValid(@PathVariable String p, @PathVariable String nid,
                                          @PathVariable String value,
                                          @RequestHeader(value = "X-Api-Key", required = false) String key) {
        return callback(p, "isValid", nid, value, key);
    }

    @PostMapping("/edc/{p}/signed/{nid}/{value}")
    public ResponseEntity<Object> signed(@PathVariable String p, @PathVariable String nid,
                                         @PathVariable String value,
                                         @RequestHeader(value = "X-Api-Key", required = false) String key) {
        return callback(p, "signed", nid, value, key);
    }

    private ResponseEntity<Object> callback(String p, String endpoint, String nid, String value, String key) {
        if (!authorized(key)) {
            return unauthorized();
        }
        if (!"true".equals(value) && !"false".equals(value)) {
            return ResponseEntity.badRequest().build();
        }
        StubState.Callback cb = state.recordCallback(p, endpoint, value, nid);
        choreography.deliver(cb);
        return ResponseEntity.noContent().build();
    }

    /** VC exchange (35888): returns the counterparty's own VC, or a test override. */
    @GetMapping("/edc/{p}/vcExchange/{nid}/request")
    public ResponseEntity<Object> vcExchange(@PathVariable String p, @PathVariable String nid,
                                             @RequestHeader(value = "X-Api-Key", required = false) String key) {
        if (!authorized(key)) {
            return unauthorized();
        }
        StubState.Callback cb = state.recordCallback(p, "vcExchange", "request", nid);
        choreography.deliver(cb);
        Negotiation n = choreography.get(nid);
        boolean askerIsProvider = n != null ? p.equals(n.providerId) : p.startsWith("provider");
        JsonNode override = askerIsProvider ? state.consumerVcOverride(nid) : state.providerVcOverride(nid);
        if (override != null) {
            if (override.path("notFound").asBoolean(false)) {
                return notFound();
            }
            return ResponseEntity.ok(override);
        }
        Participants.Participant counterparty = n != null
                ? participants.get(askerIsProvider ? n.consumerId : n.providerId)
                : participants.counterparty(p);
        if (counterparty == null || counterparty.cmUrl() == null) {
            return notFound();
        }
        Choreography.Result r = choreography.fetchVc(counterparty, nid);
        if (r.status() == 200 && r.body() != null) {
            return ResponseEntity.ok(r.body());
        }
        if (r.status() == 404) {
            return notFound();
        }
        return ResponseEntity.status(502).header("Content-Type", "application/problem+json")
                .body(Map.of("type", "urn:problem-type:simpl:badGateway", "title", "Bad Gateway", "status", 502,
                        "detail", "Counterparty contract manager did not answer"));
    }

    private ResponseEntity<Object> notFound() {
        return ResponseEntity.status(404).header("Content-Type", "application/problem+json")
                .body(Map.of("type", "urn:problem-type:simpl:notFound", "title", "Not Found", "status", 404,
                        "detail", "No credential for this negotiation"));
    }
}

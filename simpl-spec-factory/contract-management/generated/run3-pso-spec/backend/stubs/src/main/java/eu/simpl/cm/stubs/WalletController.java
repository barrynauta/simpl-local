package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One logical wallet per participant (C.4). */
@RestController
public class WalletController {

    private final Map<String, Map<String, JsonNode>> wallets = new ConcurrentHashMap<>();
    private final StubState state;

    public WalletController(StubState state) {
        this.state = state;
    }

    @PutMapping("/wallet/v1/{p}/credentials/{credentialId}")
    public ResponseEntity<Object> put(@PathVariable String p, @PathVariable String credentialId,
                                      @RequestBody JsonNode vc,
                                      @RequestHeader(value = "X-Contract-Negotiation-Id", required = false)
                                      String nidHeader) {
        String nid = nidHeader != null ? nidHeader : vc.at("/credentialSubject/contractNegotiationId").asText(null);
        if (state.hasFault(nid, "WALLET:UNAVAILABLE")) {
            return error(503, "Secure storage location unavailable");
        }
        if (state.hasFault(nid, "WALLET:FULL")) {
            return error(507, "Secure storage location is full");
        }
        if (state.hasFault(nid, "WALLET:CORRUPTED")) {
            return error(500, "Secure storage location is corrupted");
        }
        synchronized (wallets) {
            wallets.computeIfAbsent(p, k -> new LinkedHashMap<>()).put(credentialId, vc);
        }
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/wallet/v1/{p}/credentials")
    public List<JsonNode> list(@PathVariable String p,
                               @RequestParam(value = "contractNegotiationId", required = false) String nid) {
        List<JsonNode> out = new ArrayList<>();
        synchronized (wallets) {
            for (JsonNode vc : wallets.getOrDefault(p, Map.of()).values()) {
                if (nid == null || nid.equals(vc.at("/credentialSubject/contractNegotiationId").asText(null))) {
                    out.add(vc);
                }
            }
        }
        return out;
    }

    private ResponseEntity<Object> error(int status, String detail) {
        return ResponseEntity.status(status).header("Content-Type", "application/problem+json")
                .body(Map.of("type", "urn:problem-type:simpl:walletError", "title", "Wallet error", "status", status,
                        "detail", detail));
    }
}

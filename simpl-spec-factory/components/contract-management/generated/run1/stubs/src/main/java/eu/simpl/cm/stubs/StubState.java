package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.proof.DataIntegrity;
import eu.simpl.cm.common.proof.DidKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** All stub state, in memory. */
@Component
public class StubState {
    public record Participant(String participantId, String name, String did, String contactEmail, byte[] seed) {
    }

    public record CmEndpoint(String baseUrl, String apiKey) {
    }

    public record Callback(String participantId, String endpoint, Boolean value, Instant at) {
    }

    public record Call(Instant at, String method, String path, int status, JsonNode body, String nid) {
    }

    public static final Set<String> FAULTS = Set.of("SIGNER:FAIL", "VERIFIER:FAIL", "VERIFIER:REVOKED",
            "WALLET:UNAVAILABLE", "WALLET:FULL", "WALLET:CORRUPTED", "DIRECTORY:NO_CONTACT");

    final ObjectMapper mapper;
    final Map<String, Participant> directory = new LinkedHashMap<>();
    final Map<String, CmEndpoint> cms = new ConcurrentHashMap<>();
    final Map<String, ObjectNode> assets = new ConcurrentHashMap<>();
    final Map<String, Set<String>> faults = new ConcurrentHashMap<>();
    final Map<String, ObjectNode> consumerVcOverride = new ConcurrentHashMap<>();
    final Map<String, ObjectNode> providerVcOverride = new ConcurrentHashMap<>();
    final Map<String, Map<String, ObjectNode>> wallets = new ConcurrentHashMap<>();
    final Map<String, List<Callback>> callbacks = new ConcurrentHashMap<>();
    final List<Call> calls = new CopyOnWriteArrayList<>();
    final Map<String, Negotiation> negotiations = new ConcurrentHashMap<>();
    final String edcApiKey;

    public StubState(ObjectMapper mapper,
                     @Value("${CM_PROVIDER_URL:http://cm-provider:8080/contract/v1}") String providerUrl,
                     @Value("${CM_PROVIDER_KEY:test-cm-provider-key}") String providerKey,
                     @Value("${CM_CONSUMER_URL:http://cm-consumer:8080/contract/v1}") String consumerUrl,
                     @Value("${CM_CONSUMER_KEY:test-cm-consumer-key}") String consumerKey,
                     @Value("${EDC_API_KEY:test-edc-key}") String edcApiKey) {
        this.mapper = mapper;
        this.edcApiKey = edcApiKey;
        addParticipant("provider-01", "Provider One", "provider-01@example.test", 0x11);
        addParticipant("consumer-01", "Consumer One", "consumer-01@example.test", 0x22);
        addParticipant("rogue-01", "Rogue Participant", null, 0x33);
        cms.put("provider-01", new CmEndpoint(providerUrl, providerKey));
        cms.put("consumer-01", new CmEndpoint(consumerUrl, consumerKey));
        // D83 seed asset
        ObjectNode seed = mapper.createObjectNode();
        seed.put("assetId", "asset-seed-01");
        seed.put("resourceDescriptionId", "rd-asset-seed-01");
        seed.put("resourceName", "Seed resource");
        seed.put("description", "Seed asset for UI smoke tests");
        seed.put("providerId", "provider-01");
        seed.put("providerName", "Provider One");
        seed.put("consentRequired", true);
        ArrayNode policies = seed.putArray("policies");
        policies.addObject().put("type", "ACCESS").put("description", "Members of the data space only");
        policies.addObject().put("type", "USAGE").put("description", "Non-commercial use");
        seed.putArray("contractClauses");
        assets.put("asset-seed-01", seed);
    }

    private void addParticipant(String id, String name, String email, int seedByte) {
        byte[] seed = new byte[32];
        java.util.Arrays.fill(seed, (byte) seedByte);
        String did = DidKey.did(DataIntegrity.publicKeyFromSeed(seed));
        directory.put(id, new Participant(id, name, did, email, seed));
    }

    public boolean hasFault(String nid, String fault) {
        if (nid == null) {
            return false;
        }
        return faults.getOrDefault(nid, Collections.emptySet()).contains(fault);
    }

    public void addFault(String nid, String fault) {
        faults.computeIfAbsent(nid, k -> ConcurrentHashMap.newKeySet()).add(fault);
    }

    public void addCallback(String nid, Callback cb) {
        callbacks.computeIfAbsent(nid, k -> new CopyOnWriteArrayList<>()).add(cb);
    }

    public List<Callback> callbacks(String nid) {
        return callbacks.getOrDefault(nid, List.of());
    }

    public Participant byDid(String did) {
        for (Participant p : directory.values()) {
            if (p.did().equals(did)) {
                return p;
            }
        }
        return null;
    }

    public boolean trusted(String participantId) {
        return "provider-01".equals(participantId) || "consumer-01".equals(participantId);
    }

    /** The other party of a negotiation, for vcExchange. */
    public String counterparty(String nid, String participantId) {
        Negotiation n = negotiations.get(nid);
        if (n != null) {
            return participantId.equals(n.providerId) ? n.consumerId : n.providerId;
        }
        return "provider-01".equals(participantId) ? "consumer-01" : "provider-01";
    }

    public List<Call> calls(String nid) {
        List<Call> out = new ArrayList<>();
        for (Call c : calls) {
            if (nid == null || nid.equals(c.nid()) || c.path().contains(nid)) {
                out.add(c);
            }
        }
        return out;
    }
}

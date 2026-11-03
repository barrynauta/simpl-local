package eu.simpl.cm.manager.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.proof.DataIntegrity;
import eu.simpl.cm.common.proof.DidKey;
import eu.simpl.cm.common.web.HttpJson;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

/** Helpers for the integration tests: signed VCs and polling. */
final class ItSupport {
    static final ObjectMapper M = new ObjectMapper();

    private ItSupport() {
    }

    static byte[] seed(int b) {
        byte[] s = new byte[32];
        Arrays.fill(s, (byte) b);
        return s;
    }

    static String did(int b) {
        return DidKey.did(DataIntegrity.publicKeyFromSeed(seed(b)));
    }

    static ObjectNode signedVc(int seedByte, Map<String, Object> subject) {
        ObjectNode vc = M.createObjectNode();
        vc.putArray("@context").add("https://www.w3.org/ns/credentials/v2");
        vc.put("id", "urn:uuid:" + java.util.UUID.randomUUID());
        vc.putArray("type").add("VerifiableCredential").add("UsageContractCredential");
        vc.put("issuer", did(seedByte));
        vc.put("validFrom", "2026-01-01T00:00:00Z");
        vc.put("validUntil", "2099-01-01T00:00:00Z");
        vc.set("credentialSubject", M.valueToTree(subject));
        return DataIntegrity.sign(vc, seed(seedByte), "2026-10-29T10:00:00Z");
    }

    static HttpJson.Response json(int status, Object body) {
        try {
            return new HttpJson.Response(status, body == null ? null : M.writeValueAsString(body), "application/json");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static HttpJson.Response verified(boolean ok, String problem) {
        return json(200, ok ? Map.of("verified", true, "problems", java.util.List.of())
                : Map.of("verified", false, "problems", java.util.List.of(problem)));
    }

    static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.call())) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Condition not met within 10 s");
    }
}

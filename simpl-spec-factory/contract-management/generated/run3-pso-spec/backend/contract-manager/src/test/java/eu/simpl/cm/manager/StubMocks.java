package eu.simpl.cm.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.client.UibeClient;

import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/** Default answers of the mocked stub service and UIBE, close to Appendix C. */
final class StubMocks {

    static final ObjectMapper M = new ObjectMapper();
    static final String PROVIDER_DID = "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S";
    static final String CONSUMER_DID = "did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK";

    private StubMocks() {
    }

    static void defaults(StubClient stub, UibeClient uibe) {
        lenient().when(stub.callback(anyString(), anyString(), anyBoolean())).thenReturn(new HttpResult(204, null));
        lenient().when(stub.participant(any(), anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(1);
            ObjectNode p = M.createObjectNode();
            p.put("participantId", id);
            p.put("name", id.startsWith("provider") ? "Provider One" : "Consumer One");
            p.put("did", id.startsWith("provider") ? PROVIDER_DID : CONSUMER_DID);
            p.put("contactEmail", id + "@example.test");
            return new HttpResult(200, p);
        });
        lenient().when(stub.asset(any(), anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(1);
            if (id.startsWith("missing")) {
                return new HttpResult(404, null);
            }
            return new HttpResult(200, asset(id));
        });
        lenient().when(stub.issue(anyString(), any(), anyString(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> subject = inv.getArgument(1);
            String role = String.valueOf(subject.get("signerRole"));
            return new HttpResult(201, vc(M.valueToTree(subject), role.equals("PROVIDER") ? PROVIDER_DID
                    : CONSUMER_DID, inv.getArgument(2), inv.getArgument(3)));
        });
        lenient().when(stub.walletPut(anyString(), anyString(), any())).thenReturn(new HttpResult(201, null));
        lenient().when(stub.verify(anyString(), any())).thenReturn(new HttpResult(200,
                M.createObjectNode().put("verified", true).set("problems", M.createArrayNode())));
        lenient().when(uibe.postOffer(anyString(), anyString(), anyString()))
                .thenReturn(new HttpResult(201, M.createObjectNode().put("offerStatus", "CREATED")));
    }

    static ObjectNode asset(String id) {
        ObjectNode a = M.createObjectNode();
        a.put("assetId", id);
        a.put("resourceDescriptionId", "rd-" + id);
        a.put("resourceName", "Resource " + id);
        a.put("description", "Description");
        a.put("providerId", "provider-01");
        a.put("providerName", "Provider One");
        a.put("consentRequired", id.startsWith("consent"));
        a.putArray("policies").addObject().put("type", "USAGE").put("description", "Research only");
        a.putArray("contractClauses");
        return a;
    }

    static ObjectNode vc(JsonNode subject, String issuer, String validFrom, String validUntil) {
        ObjectNode vc = M.createObjectNode();
        vc.putArray("@context").add("https://www.w3.org/ns/credentials/v2");
        vc.put("id", "urn:uuid:" + UUID.randomUUID());
        vc.putArray("type").add("VerifiableCredential").add("UsageContractCredential");
        vc.put("issuer", issuer);
        vc.put("validFrom", validFrom);
        vc.put("validUntil", validUntil);
        vc.set("credentialSubject", subject);
        ObjectNode proof = vc.putObject("proof");
        proof.put("type", "DataIntegrityProof");
        proof.put("cryptosuite", "eddsa-jcs-2022");
        proof.put("proofValue", "zTest");
        return vc;
    }

    static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition not met within 10 seconds");
            }
            Thread.sleep(50);
        }
    }
}

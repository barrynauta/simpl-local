package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** C.2: the stub acting as both connectors. */
@Component
public class Choreography {
    private static final Logger log = LoggerFactory.getLogger(Choreography.class);
    private final StubState state;
    private final CmClient cm;
    private final long waitMillis;

    public Choreography(StubState state, CmClient cm, @Value("${STUB_WAIT_SECONDS:30}") long waitSeconds) {
        this.state = state;
        this.cm = cm;
        this.waitMillis = waitSeconds * 1000;
    }

    public void start(Negotiation n) {
        Thread.ofVirtual().name("negotiation-" + n.contractNegotiationId).start(() -> {
            try {
                run(n);
            } catch (Exception e) {
                log.error("Negotiation {} failed: {}", n.contractNegotiationId, e.toString(), e);
                terminate(n, "STUB_ERROR");
            }
        });
    }

    private void terminate(Negotiation n, String reason) {
        n.terminationReason = reason;
        n.enter("TERMINATED");
    }

    /** Returns false when the negotiation must stop (stopAfter reached and never advanced is fine: we block). */
    private void reached(Negotiation n, String newState) throws InterruptedException {
        n.enter(newState);
        if (newState.equals(n.stopAfter)) {
            n.advance.acquire();
        }
    }

    private StubState.Callback await(Negotiation n, int mark, long timeoutMillis, Predicate<StubState.Callback> match)
            throws InterruptedException {
        long deadline = timeoutMillis < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            List<StubState.Callback> cbs = state.callbacks(n.contractNegotiationId);
            for (int i = mark; i < cbs.size(); i++) {
                if (match.test(cbs.get(i))) {
                    return cbs.get(i);
                }
            }
            Thread.sleep(100);
        }
        return null;
    }

    private int mark(Negotiation n) {
        return state.callbacks(n.contractNegotiationId).size();
    }

    private Map<String, Object> negotiationBody(Negotiation n) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("contractNegotiationId", n.contractNegotiationId);
        m.put("assetId", n.assetId);
        m.put("providerId", n.providerId);
        m.put("consumerId", n.consumerId);
        m.put("contractOfferId", n.contractOfferId);
        return m;
    }

    private String agreementPath(Negotiation n) {
        return "/agreements/" + n.contractAgreementId + "/definitions/" + n.contractDefinitionId + "/status";
    }

    private void providerStatus(Negotiation n, String status) {
        cm.call(n.providerId, "PATCH", agreementPath(n), Map.of("status", status), n.contractNegotiationId);
    }

    void run(Negotiation n) throws InterruptedException {
        String nid = n.contractNegotiationId;
        if ("STARTED".equals(n.stopAfter)) {
            n.advance.acquire();
        }
        // 1. existence check at the provider
        int m = mark(n);
        Map<String, Object> search = new java.util.LinkedHashMap<>();
        search.put("contractAgreementId", n.contractAgreementId);
        search.put("contractDefinitionId", n.contractDefinitionId);
        search.put("assetId", n.assetId);
        search.put("providerId", n.providerId);
        search.put("consumerId", n.consumerId);
        CmClient.Result r = cm.call(n.providerId, "POST", "/agreements/" + nid + "/search", search, nid);
        if (!r.ok()) {
            terminate(n, "EXISTENCE_CHECK_FAILED");
            return;
        }
        StubState.Callback cb = await(n, m, waitMillis, c -> c.participantId().equals(n.providerId) && c.endpoint().equals("isPresent"));
        if (cb == null) {
            terminate(n, "TIMEOUT");
            return;
        }
        if (Boolean.TRUE.equals(cb.value())) {
            terminate(n, "EXISTING_CONTRACT");
            return;
        }
        reached(n, "EXISTENCE_CHECKED");

        // 2. offer at the consumer
        m = mark(n);
        r = cm.call(n.consumerId, "POST", "/credentials/agreements/" + n.contractAgreementId + "/definitions/"
                + n.contractDefinitionId + "/create", negotiationBody(n), nid);
        if (r.status() != 201) {
            providerStatus(n, "TERMINATED");
            terminate(n, "OFFER_FAILED");
            return;
        }
        reached(n, "OFFERED");

        // 3. the consumer user decides, no time limit
        cb = await(n, m, -1, c -> c.participantId().equals(n.consumerId) && c.endpoint().equals("signed"));
        if (!Boolean.TRUE.equals(cb.value())) {
            providerStatus(n, "TERMINATED");
            terminate(n, "CONSUMER_DECLINED");
            return;
        }
        reached(n, "CONSUMER_SIGNED");

        // 4. provider validates and signs
        m = mark(n);
        r = cm.call(n.providerId, "POST", "/credentials/agreements/" + n.contractAgreementId + "/definitions/"
                + n.contractDefinitionId + "/issue", negotiationBody(n), nid);
        if (r.status() != 201) {
            rejectAtConsumer(n);
            return;
        }
        cb = await(n, m, waitMillis, c -> c.participantId().equals(n.providerId)
                && (c.endpoint().equals("signed") || (c.endpoint().equals("isValid") && Boolean.FALSE.equals(c.value()))));
        if (cb == null) {
            terminate(n, "TIMEOUT");
            return;
        }
        if (!(cb.endpoint().equals("signed") && Boolean.TRUE.equals(cb.value()))) {
            rejectAtConsumer(n);
            return;
        }
        reached(n, "PROVIDER_SIGNED");

        // 5. consumer verifies the provider VC
        JsonNode providerVc;
        ObjectNode override = state.providerVcOverride.get(nid);
        if (override != null && !override.path("notFound").asBoolean(false)) {
            providerVc = override;
        } else if (override != null) {
            providerVc = null;
        } else {
            CmClient.Result vc = cm.call(n.providerId, "GET", "/verifications/" + nid + "/vc", null, nid);
            providerVc = vc.status() == 200 ? vc.body() : null;
        }
        if (providerVc == null) {
            providerStatus(n, "TERMINATED");
            terminate(n, "PROVIDER_VC_MISSING");
            return;
        }
        m = mark(n);
        r = cm.call(n.consumerId, "PUT", "/verifications/" + nid, providerVc, nid);
        if (!r.ok()) {
            providerStatus(n, "TERMINATED");
            terminate(n, "CONSUMER_REJECTED");
            return;
        }
        cb = await(n, m, waitMillis, c -> c.participantId().equals(n.consumerId) && c.endpoint().equals("isValid"));
        if (cb == null) {
            terminate(n, "TIMEOUT");
            return;
        }
        if (!Boolean.TRUE.equals(cb.value())) {
            providerStatus(n, "TERMINATED");
            terminate(n, "CONSUMER_REJECTED");
            return;
        }
        reached(n, "CONSUMER_VERIFIED");

        // 6. finalise at the provider
        providerStatus(n, "FINALIZED");
        n.enter("FINALIZED");
    }

    private void rejectAtConsumer(Negotiation n) {
        cm.call(n.consumerId, "PUT", "/contractOffers/" + n.contractNegotiationId + "/update",
                Map.of("status", "REJECTED", "additionalInfo", "Provider validation or signing failed"), n.contractNegotiationId);
        terminate(n, "PROVIDER_REJECTED");
    }
}

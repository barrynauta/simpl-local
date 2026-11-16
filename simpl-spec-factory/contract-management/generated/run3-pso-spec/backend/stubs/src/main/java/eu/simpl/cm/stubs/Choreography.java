package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The connector choreography of Appendix C.2, with the stub acting as both connectors. */
@Component
public class Choreography {

    private static final Logger LOG = LoggerFactory.getLogger(Choreography.class);

    public static final String STARTED = "STARTED";
    public static final String EXISTENCE_CHECKED = "EXISTENCE_CHECKED";
    public static final String OFFERED = "OFFERED";
    public static final String CONSUMER_SIGNED = "CONSUMER_SIGNED";
    public static final String PROVIDER_SIGNED = "PROVIDER_SIGNED";
    public static final String CONSUMER_VERIFIED = "CONSUMER_VERIFIED";
    public static final String FINALIZED = "FINALIZED";
    public static final String TERMINATED = "TERMINATED";

    private final Map<String, Negotiation> negotiations = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Participants participants;
    private final StubState state;
    private final ObjectMapper mapper;
    private final RestClient http;
    private final long waitMillis;

    public Choreography(Participants participants, StubState state, ObjectMapper mapper,
                        @Value("${stub.wait-millis:30000}") long waitMillis) {
        this.participants = participants;
        this.state = state;
        this.mapper = mapper;
        this.waitMillis = waitMillis;
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000)).version(HttpClient.Version.HTTP_1_1).build());
        f.setReadTimeout(Duration.ofMillis(20000));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public Negotiation get(String nid) {
        return negotiations.get(nid);
    }

    public Negotiation start(Negotiation n) {
        negotiations.put(n.contractNegotiationId, n);
        n.enter(STARTED);
        executor.submit(() -> {
            try {
                run(n);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("Negotiation {} failed in the stub", n.contractNegotiationId, e);
                terminate(n, "STUB_ERROR");
            }
        });
        return n;
    }

    /** Called by the connector endpoints for every callback. */
    public void deliver(StubState.Callback cb) {
        Negotiation n = negotiations.get(cb.nid());
        if (n != null) {
            n.deliver(cb);
        }
    }

    private void run(Negotiation n) throws InterruptedException {
        Participants.Participant provider = participants.get(n.providerId);
        Participants.Participant consumer = participants.get(n.consumerId);
        if (provider == null || consumer == null || provider.cmUrl() == null || consumer.cmUrl() == null) {
            terminate(n, "UNKNOWN_PARTICIPANT");
            return;
        }
        n.pauseIfRequested(STARTED);

        // Step 1: existence check at the Provider.
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("contractAgreementId", n.contractAgreementId);
        search.put("contractDefinitionId", n.contractDefinitionId);
        search.put("assetId", n.assetId);
        search.put("providerId", n.providerId);
        search.put("consumerId", n.consumerId);
        Result r = call(provider, "POST", "/agreements/" + n.contractNegotiationId + "/search", search, n);
        if (r.status != 200) {
            terminate(n, "SEARCH_FAILED");
            return;
        }
        StubState.Callback present = n.await(cb -> cb.participantId().equals(n.providerId)
                && cb.endpoint().equals("isPresent"), waitMillis);
        if (present == null) {
            terminate(n, "TIMEOUT");
            return;
        }
        if ("true".equals(present.value())) {
            terminate(n, "EXISTING_CONTRACT");
            return;
        }
        n.enter(EXISTENCE_CHECKED);
        n.pauseIfRequested(EXISTENCE_CHECKED);

        // Step 2: Consumer CM prepares the offer.
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("contractNegotiationId", n.contractNegotiationId);
        create.put("assetId", n.assetId);
        create.put("providerId", n.providerId);
        create.put("consumerId", n.consumerId);
        create.put("contractOfferId", n.contractOfferId);
        r = call(consumer, "POST", "/credentials/agreements/" + n.contractAgreementId + "/definitions/"
                + n.contractDefinitionId + "/create", create, n);
        if (r.status != 201) {
            terminate(n, "OFFER_FAILED");
            providerStatus(provider, n, "TERMINATED");
            return;
        }
        n.enter(OFFERED);
        n.pauseIfRequested(OFFERED);

        // Step 3: wait for the Consumer user's decision (no time limit).
        StubState.Callback signed = n.await(cb -> cb.participantId().equals(n.consumerId)
                && cb.endpoint().equals("signed"), 0);
        if (!"true".equals(signed.value())) {
            providerStatus(provider, n, "TERMINATED");
            terminate(n, "CONSUMER_DECLINED");
            return;
        }
        n.enter(CONSUMER_SIGNED);
        n.pauseIfRequested(CONSUMER_SIGNED);

        // Step 4: Provider validates and signs.
        r = call(provider, "POST", "/credentials/agreements/" + n.contractAgreementId + "/definitions/"
                + n.contractDefinitionId + "/issue", create, n);
        StubState.Callback outcome = null;
        if (r.status == 201) {
            outcome = n.await(cb -> cb.participantId().equals(n.providerId)
                    && (cb.endpoint().equals("signed") || (cb.endpoint().equals("isValid")
                    && "false".equals(cb.value()))), waitMillis);
            if (outcome == null) {
                terminate(n, "TIMEOUT");
                return;
            }
        }
        if (outcome == null || !(outcome.endpoint().equals("signed") && "true".equals(outcome.value()))) {
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("status", "REJECTED");
            update.put("additionalInfo", "Provider validation or signing failed");
            call(consumer, "PUT", "/contractOffers/" + n.contractNegotiationId + "/update", update, n);
            terminate(n, "PROVIDER_REJECTED");
            return;
        }
        n.enter(PROVIDER_SIGNED);
        n.pauseIfRequested(PROVIDER_SIGNED);

        // Step 5: Consumer verifies the Provider VC.
        JsonNode providerVc = state.providerVcOverride(n.contractNegotiationId);
        if (providerVc != null && providerVc.path("notFound").asBoolean(false)) {
            providerVc = mapper.createObjectNode();
        } else if (providerVc == null) {
            Result vc = call(provider, "GET", "/verifications/" + n.contractNegotiationId + "/vc", null, n);
            providerVc = vc.status == 200 && vc.body != null ? vc.body : mapper.createObjectNode();
        }
        r = call(consumer, "PUT", "/verifications/" + n.contractNegotiationId, providerVc, n);
        StubState.Callback valid = null;
        if (r.status == 200) {
            valid = n.await(cb -> cb.participantId().equals(n.consumerId)
                    && cb.endpoint().equals("isValid"), waitMillis);
            if (valid == null) {
                terminate(n, "TIMEOUT");
                return;
            }
        }
        if (valid == null || !"true".equals(valid.value())) {
            providerStatus(provider, n, "TERMINATED");
            terminate(n, "CONSUMER_REJECTED");
            return;
        }
        n.enter(CONSUMER_VERIFIED);
        n.pauseIfRequested(CONSUMER_VERIFIED);

        // Step 6: finalise at the Provider.
        providerStatus(provider, n, "FINALIZED");
        n.enter(FINALIZED);
    }

    private void providerStatus(Participants.Participant provider, Negotiation n, String status) {
        call(provider, "PATCH", "/agreements/" + n.contractAgreementId + "/definitions/" + n.contractDefinitionId
                + "/status", Map.of("status", status), n);
    }

    private void terminate(Negotiation n, String reason) {
        n.terminationReason = reason;
        n.enter(TERMINATED);
    }

    record Result(int status, JsonNode body) {
    }

    /** Calls a CM with its API key (D77), X-Correlation-Id = nid and the nid header (C.2, D80). */
    Result call(Participants.Participant target, String method, String path, Object body, Negotiation n) {
        try {
            RestClient.RequestBodySpec spec = http.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(target.cmUrl() + path)
                    .header("X-Api-Key", target.cmApiKey())
                    .header("X-Correlation-Id", n.contractNegotiationId)
                    .header("X-Contract-Negotiation-Id", n.contractNegotiationId)
                    .accept(MediaType.APPLICATION_JSON, MediaType.valueOf("application/problem+json"));
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((req, res) -> {
                HttpStatusCode code = res.getStatusCode();
                byte[] bytes = res.getBody().readAllBytes();
                JsonNode json = null;
                if (bytes.length > 0) {
                    try {
                        json = mapper.readTree(bytes);
                    } catch (Exception e) {
                        json = null;
                    }
                }
                return new Result(code.value(), json);
            });
        } catch (Exception e) {
            LOG.warn("Stub call {} {} failed: {}", method, path, e.toString());
            return new Result(599, null);
        }
    }

    /** Fetches a VC from a CM for the vcExchange endpoint. */
    Result fetchVc(Participants.Participant target, String nid) {
        Negotiation fake = new Negotiation(nid, null, null, null, null, null, null, null);
        return call(target, "GET", "/verifications/" + nid + "/vc", null, fake);
    }

    ObjectNode describe(Negotiation n) {
        ObjectNode o = mapper.createObjectNode();
        o.put("contractNegotiationId", n.contractNegotiationId);
        o.put("state", n.state);
        o.put("terminationReason", n.terminationReason);
        o.set("steps", mapper.valueToTree(n.steps));
        var cbs = mapper.createArrayNode();
        for (StubState.Callback cb : state.callbacks(n.contractNegotiationId)) {
            ObjectNode c = mapper.createObjectNode();
            c.put("participantId", cb.participantId());
            c.put("endpoint", cb.endpoint());
            c.put("value", cb.value());
            c.put("at", cb.at());
            cbs.add(c);
        }
        o.set("callbacks", cbs);
        return o;
    }
}

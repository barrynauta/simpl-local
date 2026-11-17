package eu.simpl.cm.manager.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.CorrelationIdFilter;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.manager.CmSettings;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Calls to the stub: connector callbacks, issuer, verifier, wallet, catalogue, directory (Appendix C). */
@Component
public class StubClient {
    private final CmSettings settings;
    private final ObjectMapper mapper;
    private final HttpJson http;

    public StubClient(CmSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = new HttpJson(mapper, Duration.ofSeconds(10));
    }

    private Map<String, String> headers(String nid, boolean edc) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put(CorrelationIdFilter.HEADER, CorrelationIdFilter.current());
        if (nid != null) {
            h.put("X-Contract-Negotiation-Id", nid);
        }
        if (edc) {
            h.put("X-Api-Key", settings.stubEdcApiKey);
        }
        return h;
    }

    private String base() {
        return settings.stubBaseUrl;
    }

    /** POST /edc/{own}/{endpoint}/{nid}/{value}; endpoint is isPresent, isValid or signed. */
    public HttpJson.Response callback(String endpoint, String nid, boolean value) {
        return http.send("POST", base() + "/edc/" + settings.participantId + "/" + endpoint + "/" + enc(nid) + "/" + value,
                headers(nid, true), null);
    }

    public HttpJson.Response vcExchange(String nid) {
        return http.send("GET", base() + "/edc/" + settings.participantId + "/vcExchange/" + enc(nid) + "/request",
                headers(nid, true), null);
    }

    public HttpJson.Response issue(String nid, Map<String, Object> subject, String validFrom, String validUntil) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("participantId", settings.participantId);
        body.put("credentialSubject", subject);
        body.put("validFrom", validFrom);
        body.put("validUntil", validUntil);
        return http.send("POST", base() + "/vc-issuer/v1/credentials/issue", headers(nid, false), body);
    }

    public HttpJson.Response verify(String nid, JsonNode credential) {
        return http.send("POST", base() + "/vc-issuer/v1/credentials/verify", headers(nid, false), Map.of("credential", credential));
    }

    public HttpJson.Response storeInWallet(String nid, String credentialId, JsonNode credential) {
        return http.send("PUT", base() + "/wallet/v1/" + settings.participantId + "/credentials/" + enc(credentialId),
                headers(nid, false), credential);
    }

    public HttpJson.Response asset(String nid, String assetId) {
        return http.send("GET", base() + "/catalogue/v1/assets/" + enc(assetId), headers(nid, false), null);
    }

    public HttpJson.Response participant(String nid, String participantId) {
        return http.send("GET", base() + "/directory/v1/participants/" + enc(participantId), headers(nid, false), null);
    }

    public JsonNode json(HttpJson.Response r) {
        return r.json(mapper);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

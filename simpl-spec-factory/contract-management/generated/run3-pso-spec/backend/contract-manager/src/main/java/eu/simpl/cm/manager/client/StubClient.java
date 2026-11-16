package eu.simpl.cm.manager.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.manager.CmProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls to the stub service: connector callbacks (C.1), VC issuer and verifier (C.3), wallet (C.4),
 * catalogue and directory (C.5). Every call carries X-Contract-Negotiation-Id (D80).
 */
@Component
public class StubClient {

    private final CmProperties props;
    private final Http http;

    public StubClient(CmProperties props, ObjectMapper mapper) {
        this.props = props;
        this.http = new Http(mapper, 3000, 10000);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Map<String, String> headers(String nid, boolean edc) {
        Map<String, String> h = new LinkedHashMap<>();
        if (nid != null) {
            h.put("X-Contract-Negotiation-Id", nid);
        }
        if (edc) {
            h.put("X-Api-Key", props.stubEdcApiKey());
        }
        return h;
    }

    private String base() {
        return props.stubBaseUrl();
    }

    /** POST /edc/{p}/{endpoint}/{nid}/{value} for isPresent, isValid and signed. */
    public HttpResult callback(String endpoint, String nid, boolean value) {
        return http.exchange(HttpMethod.POST, base() + "/edc/" + enc(props.participantId()) + "/" + endpoint + "/"
                + enc(nid) + "/" + value, headers(nid, true), null);
    }

    public HttpResult vcExchange(String nid) {
        return http.exchange(HttpMethod.GET, base() + "/edc/" + enc(props.participantId()) + "/vcExchange/"
                + enc(nid) + "/request", headers(nid, true), null);
    }

    public HttpResult issue(String nid, Map<String, Object> subject, String validFrom, String validUntil) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("participantId", props.participantId());
        body.put("credentialSubject", subject);
        body.put("validFrom", validFrom);
        body.put("validUntil", validUntil);
        return http.exchange(HttpMethod.POST, base() + "/vc-issuer/v1/credentials/issue", headers(nid, false), body);
    }

    public HttpResult verify(String nid, JsonNode credential) {
        return http.exchange(HttpMethod.POST, base() + "/vc-issuer/v1/credentials/verify", headers(nid, false),
                Map.of("credential", credential));
    }

    public HttpResult walletPut(String nid, String credentialId, JsonNode vc) {
        return http.exchange(HttpMethod.PUT, base() + "/wallet/v1/" + enc(props.participantId()) + "/credentials/"
                + enc(credentialId), headers(nid, false), vc);
    }

    public HttpResult asset(String nid, String assetId) {
        return http.exchange(HttpMethod.GET, base() + "/catalogue/v1/assets/" + enc(assetId), headers(nid, false),
                null);
    }

    public HttpResult participant(String nid, String participantId) {
        return http.exchange(HttpMethod.GET, base() + "/directory/v1/participants/" + enc(participantId),
                headers(nid, false), null);
    }
}

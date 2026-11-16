package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.common.Ids;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** UIBE read model and lifecycle API for the UI (D7, B.1 to B.3). */
@RestController
public class ContractsController {

    private final CmClient cm;
    private final OfferService offers;
    private final UibeProperties props;
    private final ObjectMapper mapper;

    public ContractsController(CmClient cm, OfferService offers, UibeProperties props, ObjectMapper mapper) {
        this.cm = cm;
        this.offers = offers;
        this.props = props;
        this.mapper = mapper;
    }

    @GetMapping(value = "/contracts", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode list(@RequestParam(value = "contractNegotiationId", required = false) String nid,
                         @RequestParam(value = "resourceName", required = false) String resourceName,
                         @RequestParam(value = "providerName", required = false) String providerName,
                         @RequestParam(value = "consumerName", required = false) String consumerName,
                         @RequestParam(value = "counterpartyName", required = false) String counterpartyName,
                         @RequestParam(value = "status", required = false) String status) {
        if (counterpartyName != null && !counterpartyName.isEmpty()) {
            if (props.isConsumerParty()) {
                providerName = counterpartyName;
            } else {
                consumerName = counterpartyName;
            }
        }
        UriComponentsBuilder b = cm.uri("/contracts");
        param(b, "contractNegotiationId", nid);
        param(b, "resourceName", resourceName);
        param(b, "providerName", providerName);
        param(b, "consumerName", consumerName);
        param(b, "status", status);
        return cm.require(HttpMethod.GET, b.encode().build().toUri(), null, null);
    }

    private static void param(UriComponentsBuilder b, String name, String value) {
        if (value != null && !value.isEmpty()) {
            b.queryParam(name, value);
        }
    }

    @GetMapping(value = "/contracts/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> details(@PathVariable("nid") String nid, Authentication auth) {
        Ids.requirePathId("contractNegotiationId", nid);
        JsonNode d = cm.require(HttpMethod.GET, cm.uri("/contracts/" + enc(nid)).build(true).toUri(), null, null);
        return enrich(d, auth);
    }

    @PostMapping(value = "/contracts/{nid}/lifecycleActions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> act(@PathVariable("nid") String nid, @RequestBody(required = false) JsonNode body,
                                   Authentication auth) {
        Ids.requirePathId("contractNegotiationId", nid);
        if (body == null || !body.isObject()) {
            throw ApiException.validation("Request body must be a JSON object");
        }
        Map<String, Object> forward = new LinkedHashMap<>();
        forward.put("action", body.get("action"));
        forward.put("reason", body.get("reason"));
        forward.put("actor", auth.getName());
        JsonNode d = cm.require(HttpMethod.POST, cm.uri("/contracts/" + enc(nid) + "/lifecycleActions").build(true)
                .toUri(), forward, auth.getName());
        return enrich(d, auth);
    }

    private Map<String, Object> enrich(JsonNode d, Authentication auth) {
        Map<String, Object> m = CmClient.asMap(mapper, d);
        String nid = String.valueOf(m.get("contractNegotiationId"));
        Optional<Offer> o = offers.find(nid);
        m.put("offerStatus", o.map(x -> x.status).orElse(null));
        m.put("humanReadableData", o.map(x -> x.humanReadable).orElse(null));
        m.put("hashData", o.map(x -> x.hash).orElse(null));
        boolean consentRole = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(SecurityConfig.ROLE_CONSENT_MANAGER::equals);
        List<String> allowed = new ArrayList<>();
        if (m.get("allowedActions") instanceof List<?> l) {
            for (Object a : l) {
                if (!"UPDATE_CONSENT".equals(a) || consentRole) {
                    allowed.add(String.valueOf(a));
                }
            }
        }
        m.put("allowedActions", allowed);
        if (allowed.contains("UPDATE_CONSENT")) {
            m.put("consentManagerUrl", props.consentManagerUrl() + "?contractNegotiationId="
                    + URLEncoder.encode(nid, StandardCharsets.UTF_8));
        }
        return m;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

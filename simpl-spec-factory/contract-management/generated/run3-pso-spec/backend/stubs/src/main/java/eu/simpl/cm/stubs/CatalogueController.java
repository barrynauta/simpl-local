package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Catalogue, participant directory and consent manager page (C.5), plus the asset control endpoint. */
@RestController
public class CatalogueController {

    private final Map<String, ObjectNode> assets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final Participants participants;
    private final StubState state;

    public CatalogueController(ObjectMapper mapper, Participants participants, StubState state) {
        this.mapper = mapper;
        this.participants = participants;
        this.state = state;
        ObjectNode seed = mapper.createObjectNode();
        seed.put("assetId", "asset-seed-01");
        seed.put("resourceName", "Seed resource");
        seed.put("description", "Seed asset for UI smoke tests");
        seed.put("providerId", "provider-01");
        seed.put("consentRequired", true);
        seed.set("policies", mapper.valueToTree(List.of(
                Map.of("type", "ACCESS", "description", "Access for registered consumers"),
                Map.of("type", "USAGE", "description", "Use for research only"))));
        seed.set("contractClauses", mapper.valueToTree(List.of(
                Map.of("documentName", "general-terms.pdf",
                        "hash", "3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b"))));
        assets.put("asset-seed-01", normalise(seed));
    }

    private ObjectNode normalise(ObjectNode a) {
        String id = a.path("assetId").asText();
        if (!a.path("resourceDescriptionId").isTextual()) {
            a.put("resourceDescriptionId", "rd-" + id);
        }
        if (!a.path("consentRequired").isBoolean()) {
            a.put("consentRequired", false);
        }
        if (!a.path("providerName").isTextual()) {
            Participants.Participant p = participants.get(a.path("providerId").asText(null));
            if (p != null) {
                a.put("providerName", p.name());
            } else {
                a.putNull("providerName");
            }
        }
        if (!a.path("resourceName").isTextual()) {
            a.put("resourceName", id);
        }
        if (!a.has("description")) {
            a.putNull("description");
        }
        if (!a.path("policies").isArray()) {
            a.putArray("policies");
        }
        if (!a.path("contractClauses").isArray()) {
            a.putArray("contractClauses");
        }
        return a;
    }

    @PostMapping("/_control/catalogue/assets")
    public ResponseEntity<Object> register(@RequestBody JsonNode body) {
        if (!body.isObject() || !body.path("assetId").isTextual() || body.get("assetId").asText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "assetId is required"));
        }
        ObjectNode a = normalise(((ObjectNode) body).deepCopy());
        assets.put(a.get("assetId").asText(), a);
        return ResponseEntity.status(201).body(a);
    }

    @GetMapping("/catalogue/v1/assets/{assetId}")
    public ResponseEntity<Object> asset(@PathVariable String assetId) {
        ObjectNode a = assets.get(assetId);
        if (a == null) {
            return ResponseEntity.status(404).body(Map.of("status", 404, "title", "Not Found",
                    "detail", "Unknown asset"));
        }
        return ResponseEntity.ok(a);
    }

    @GetMapping("/directory/v1/participants/{p}")
    public ResponseEntity<Object> participant(@PathVariable String p,
                                              @RequestHeader(value = "X-Contract-Negotiation-Id", required = false)
                                              String nid) {
        Participants.Participant x = participants.get(p);
        if (x == null) {
            return ResponseEntity.status(404).body(Map.of("status", 404, "title", "Not Found",
                    "detail", "Unknown participant"));
        }
        ObjectNode o = mapper.createObjectNode();
        o.put("participantId", x.id());
        o.put("name", x.name());
        o.put("did", x.did());
        if (x.contactEmail() == null || state.hasFault(nid, "DIRECTORY:NO_CONTACT")) {
            o.putNull("contactEmail");
        } else {
            o.put("contactEmail", x.contactEmail());
        }
        return ResponseEntity.ok(o);
    }

    @GetMapping(value = "/consent", produces = MediaType.TEXT_HTML_VALUE)
    public String consent(@RequestParam(value = "contractNegotiationId", required = false) String nid) {
        String safe = nid == null ? "" : HtmlUtils.htmlEscape(nid);
        return "<!DOCTYPE html><html><head><title>Consent manager stub</title></head><body>"
                + "<h1>Consent manager stub</h1><p>Contract negotiation: <span id=\"nid\">" + safe + "</span></p>"
                + "<p>This page does not change anything.</p></body></html>";
    }
}

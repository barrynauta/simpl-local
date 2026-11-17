package eu.simpl.catalogue.qma.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.RdProjection;
import eu.simpl.catalogue.common.Roles;
import eu.simpl.catalogue.qma.client.CatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Participant operations on own RDs (BP 5B.1, D16, D19, D21). */
@RestController
@RequestMapping("/v1/participants/resourceDescriptions")
public class ParticipantController {
    private final CatClient cat;

    public ParticipantController(CatClient cat) {
        this.cat = cat;
    }

    private static Caller publisher() {
        Caller c = Caller.current().requireAny(Roles.PUBLISHER);
        if (c.participantId() == null) throw ApiException.forbidden("The access token has no participant_id.");
        return c;
    }

    private static long uploadMs(JsonNode item) {
        try {
            return java.time.Instant.parse(item.path("meta").path("uploadDatetime").asText()).toEpochMilli();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<String> list(@RequestParam(required = false) String orderBy,
                                       @RequestParam(required = false) String page,
                                       @RequestParam(required = false) String pageSize) {
        Caller c = publisher();
        String order = orderBy == null || orderBy.isEmpty() ? "publicationDate" : orderBy;
        if (!order.equals("publicationDate") && !order.equals("resourceType")) {
            throw ApiException.invalidInput("orderBy must be publicationDate or resourceType.");
        }
        Paging p = Paging.parse(page, pageSize);
        List<JsonNode> items = new ArrayList<>(cat.ownedBy(c.bearer(), c.participantId()));
        Comparator<JsonNode> byDate = Comparator.comparingLong(ParticipantController::uploadMs).reversed()
                .thenComparing(i -> i.path("meta").path("id").asText());
        Comparator<JsonNode> cmp = order.equals("resourceType")
                ? Comparator.<JsonNode, String>comparing(i -> RdProjection.resourceType(Json.parse(i.path("content").asText())))
                    .thenComparing(byDate)
                : byDate;
        items.sort(cmp);
        List<JsonNode> pageItems = new ArrayList<>();
        items.stream().skip(p.offset()).limit(p.pageSize()).forEach(i -> {
            ObjectNode n = RdProjection.mainProperties(Json.parse(i.path("content").asText()));
            JsonNode overall = i.path("meta").path("qualityAssessment").path("overall");
            if (overall.path("score").isNumber()) n.put("overallScore", overall.get("score").asDouble()); else n.putNull("overallScore");
            n.put("overallClassification", overall.path("classification").asText(null));
            ObjectNode w = Json.object();
            w.set("n", n);
            pageItems.add(w);
        });
        return Support.json(Support.page(items.size(), pageItems, p, Http.query().add("orderBy", order),
                "/participants/resourceDescriptions", false));
    }

    private JsonNode owned(Caller c, String id) {
        JsonNode item = cat.find(c.bearer(), id).orElseThrow(() ->
                new ApiException(ProblemTypes.NOT_FOUND, "Resource with given id was not found."));
        JsonNode vc = Json.parse(item.path("content").asText());
        if (!c.participantId().equals(RdProjection.providedBy(vc))) {
            throw ApiException.forbidden("Client is not authorized to access the requested resource.");
        }
        return item;
    }

    @GetMapping(value = "/{resourceDescriptionId}", produces = "application/json")
    public ResponseEntity<String> get(@PathVariable String resourceDescriptionId) {
        Caller c = publisher();
        JsonNode item = owned(c, resourceDescriptionId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(item.path("content").asText());
    }

    @PatchMapping("/{resourceDescriptionId}")
    public ResponseEntity<Void> revoke(@PathVariable String resourceDescriptionId) {
        Caller c = publisher();
        owned(c, resourceDescriptionId);
        try {
            cat.revoke(c.bearer(), resourceDescriptionId);
        } catch (ApiException e) {
            if (e.status() == 404) throw new ApiException(ProblemTypes.NOT_FOUND, "Resource with given id was not found.");
            if (e.status() == 403) throw ApiException.forbidden("Client is not authorized to access the requested resource.");
            throw e;
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{resourceDescriptionId}/version", produces = "application/json")
    public ResponseEntity<String> versions(@PathVariable String resourceDescriptionId,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Caller c = publisher();
        Paging p = Paging.parse(page, pageSize);
        JsonNode item = owned(c, resourceDescriptionId);
        String key = item.path("meta").path("resourceKey").asText();
        List<JsonNode> all = new ArrayList<>(cat.ownedBy(c.bearer(), c.participantId()));
        all.removeIf(i -> !key.equals(i.path("meta").path("resourceKey").asText()));
        all.sort(Comparator.<JsonNode>comparingInt(i -> i.path("meta").path("currentVersion").asInt()).reversed());
        ObjectNode out = Json.object();
        out.put("totalCount", all.size());
        ArrayNode arr = out.putArray("items");
        all.stream().skip(p.offset()).limit(p.pageSize()).forEach(i -> {
            ObjectNode n = RdProjection.mainProperties(Json.parse(i.path("content").asText()));
            n.put("version", i.path("meta").path("currentVersion").asInt());
            arr.addObject().set("n", n);
        });
        return Support.json(out);
    }
}

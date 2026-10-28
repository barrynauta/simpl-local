package eu.simpl.catalogue.cbe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cbe.CbeProps;
import eu.simpl.catalogue.cbe.Downstream;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/** The grouped, consolidated search response of D38, D39 and section 6.2. One qma call per catalogue. */
@Component
public class GroupedSearch {
    /** A planned call: the qma request for one catalogue. */
    public record Call(HttpMethod method, URI uri, String body) {}

    private final Downstream down;
    private final FederationConfig cfg;
    private final CbeProps props;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public GroupedSearch(Downstream down, FederationConfig cfg, CbeProps props) {
        this.down = down;
        this.cfg = cfg;
        this.props = props;
    }

    public String qma() { return props.getQmaUrl(); }

    /** catalogueIds from repeated or comma-separated values; missing means own only (D32). */
    public List<String> resolve(List<String> raw) {
        List<String> ids = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            ids.add(cfg.ownCatalogue.id);
            return ids;
        }
        for (String r : raw) {
            if (r == null || r.isBlank()) throw ApiException.invalidInput("Each catalogueIds value must be a non-empty string.");
            String id = cfg.isOwn(r) ? cfg.ownCatalogue.id : r;
            if (!cfg.isOwn(id) && cfg.federated(id).isEmpty()) {
                throw ApiException.invalidInput("Unknown catalogue id '" + r + "'.");
            }
            if (!ids.contains(id)) ids.add(id);
        }
        if (ids.remove(cfg.ownCatalogue.id)) ids.add(0, cfg.ownCatalogue.id);
        return ids;
    }

    public ObjectNode run(List<String> ids, Paging p, String bearer, Function<String, Call> plan) {
        List<CompletableFuture<Downstream.Res>> futures = new ArrayList<>();
        for (String id : ids) {
            Call c = plan.apply(id);
            futures.add(CompletableFuture.supplyAsync(() -> down.call(c.method(), c.uri(), bearer, c.body(), "application/json"), pool));
        }
        ObjectNode out = Json.object();
        ArrayNode flat = Json.array();
        ArrayNode groups = Json.array();
        long totalCount = 0;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            boolean own = cfg.isOwn(id);
            Downstream.Res res;
            try {
                res = futures.get(i).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof ApiException ae) {
                    if (own) throw ae;
                    groups.add(failedGroup(id, "UNAVAILABLE", ProblemTypes.FEDERATION_UNAVAILABLE.uri, ae.getMessage(), p));
                    continue;
                }
                throw new ApiException(ProblemTypes.INTERNAL, "The search failed.");
            }
            if (!res.ok()) {
                JsonNode problem = Json.parseOrNull(res.body());
                if (res.status() == 400) {
                    throw new ApiException(400, Http.problemBody(400, res.body()));
                }
                if (own) throw new ApiException(res.status() >= 500 ? 500 : res.status(), Http.problemBody(res.status(), res.body()));
                String type = problem == null ? ProblemTypes.FEDERATION_UNAVAILABLE.uri
                        : problem.path("type").asText(ProblemTypes.FEDERATION_UNAVAILABLE.uri);
                String detail = problem == null ? "The catalogue is not available." : problem.path("detail").asText("");
                String status = type.equals(ProblemTypes.FEDERATION_UNAUTHENTICATED.uri) ? "UNAUTHENTICATED" : "UNAVAILABLE";
                groups.add(failedGroup(id, status, type, detail, p));
                continue;
            }
            JsonNode body = Json.parseOrNull(res.body());
            ObjectNode g = group(id, "OK", null, p);
            ArrayNode items = g.putArray("items");
            long total = body == null ? 0 : body.has("total") ? body.path("total").asLong() : body.path("totalCount").asLong();
            if (body != null) {
                for (JsonNode item : body.path("items")) {
                    ObjectNode mapped = item(item.path("n"), id, own);
                    items.add(mapped);
                    flat.add(mapped.deepCopy());
                }
            }
            g.put("total", total);
            g.put("totalPages", p.totalPages(total));
            totalCount += total;
            groups.add(g);
        }
        out.put("totalCount", totalCount);
        out.set("items", flat);
        out.set("groups", groups);
        return out;
    }

    private ObjectNode item(JsonNode n, String catalogueId, boolean own) {
        ObjectNode i = Json.object();
        i.set("claimsGraphUri", n.has("claimsGraphUri") ? n.get("claimsGraphUri") : Json.array());
        for (String k : List.of("offeringType", "name", "description", "inLanguage", "serviceAccessPoint")) {
            JsonNode v = n.get(k);
            if (v == null || v.isNull()) i.putNull(k); else i.set(k, v);
        }
        JsonNode s = n.get("overallScore");
        if (own && s != null && s.isNumber()) i.put("qualityScore", s.asDouble()); else i.putNull("qualityScore");
        JsonNode c = n.get("overallClassification");
        if (own && c != null && c.isTextual()) i.put("qualityClassification", c.asText()); else i.putNull("qualityClassification");
        i.put("catalogueId", catalogueId);
        ObjectNode w = Json.object();
        w.set("i", i);
        return w;
    }

    private ObjectNode group(String id, String status, ObjectNode error, Paging p) {
        ObjectNode g = Json.object();
        boolean own = cfg.isOwn(id);
        g.put("catalogueId", id);
        if (own) {
            g.put("catalogueName", cfg.ownCatalogue.name);
            g.put("governanceAuthorityId", cfg.gaId);
            g.putNull("authorityURL");
        } else {
            FederationConfig.FederatedCatalogue c = cfg.federated(id).orElseThrow();
            g.put("catalogueName", c.name);
            g.put("governanceAuthorityId", c.gaId);
            g.put("authorityURL", c.authorityURL);
        }
        g.put("local", own);
        g.put("status", status);
        if (error == null) g.putNull("error"); else g.set("error", error);
        g.put("page", p.page());
        g.put("pageSize", p.pageSize());
        return g;
    }

    private ObjectNode failedGroup(String id, String status, String type, String detail, Paging p) {
        ObjectNode err = Json.object();
        err.put("type", type);
        err.put("detail", detail);
        ObjectNode g = group(id, status, err, p);
        g.put("total", 0);
        g.put("totalPages", 0);
        g.putArray("items");
        return g;
    }
}

package eu.simpl.catalogue.qma.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.QualityParams;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validation and response helpers shared by the qma controllers. */
final class Support {
    static final Pattern SEARCH_STRING = Pattern.compile("^(?=.*\\S)[\\p{L}\\p{N}\\s.,'\\-_&@()+/]{1,255}$");

    private Support() {}

    static String searchString(String s) {
        if (s == null || s.isEmpty() || s.length() > 255 || !SEARCH_STRING.matcher(s).matches()) {
            throw ApiException.invalidInput("searchString is required, must not be blank, must be at most 255 characters, "
                    + "and must contain only permitted characters.");
        }
        return s;
    }

    /** catalogueIds as a repeated query parameter (style form, explode true). */
    static List<String> catalogueIds(String[] values) {
        if (values == null) {
            throw ApiException.invalidInput("Required query parameter 'catalogueIds' is missing; select at least one catalogue.");
        }
        return catalogueIds(List.of(values));
    }

    static List<String> catalogueIds(List<String> values) {
        if (values.isEmpty()) throw ApiException.invalidInput("catalogueIds must contain at least one catalogue identifier.");
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v == null || v.isBlank()) throw ApiException.invalidInput("Each catalogueIds value must be a non-empty string.");
            if (!out.contains(v)) out.add(v);
        }
        return out;
    }

    static List<String> catalogueIdsFromBody(JsonNode body, boolean required) {
        JsonNode ids = body.get("catalogueIds");
        if (ids == null || ids.isNull()) {
            if (required) throw ApiException.invalidInput("Required field 'catalogueIds' is missing; select at least one catalogue.");
            return List.of();
        }
        if (!ids.isArray()) throw ApiException.invalidInput("catalogueIds must be an array of catalogue identifiers.");
        List<String> out = new ArrayList<>();
        for (JsonNode n : ids) {
            if (!n.isTextual() || n.asText().isBlank()) throw ApiException.invalidInput("Each catalogueIds value must be a non-empty string.");
            out.add(n.asText());
        }
        return out;
    }

    /** Resolves ids to "own" (null value) or a configured federated catalogue; unknown ids are 400 (D40). */
    static void checkKnown(FederationConfig cfg, List<String> ids) {
        for (String id : ids) {
            if (!cfg.isOwn(id) && cfg.federated(id).isEmpty()) {
                throw ApiException.invalidInput("Unknown catalogue id '" + id + "'.");
            }
        }
    }

    static JsonNode body(String text) {
        JsonNode root = Json.parseOrNull(text);
        if (root == null || !root.isObject()) throw ApiException.badRequest("The request body must be a JSON object.");
        return root;
    }

    /** searchResponsePayload of the adapter contract (D31). */
    static ObjectNode page(long total, List<JsonNode> items, Paging p, Http.Query nextQuery, String path, boolean nullScores) {
        ObjectNode out = Json.object();
        out.put("totalCount", total);
        ArrayNode arr = out.putArray("items");
        for (JsonNode i : items) {
            JsonNode copy = i.deepCopy();
            if (nullScores && copy.path("n").isObject()) {
                ((ObjectNode) copy.get("n")).putNull("overallScore");
                ((ObjectNode) copy.get("n")).putNull("overallClassification");
            }
            arr.add(copy);
        }
        int totalPages = p.totalPages(total);
        out.put("page", p.page());
        out.put("pageSize", p.pageSize());
        out.put("total", total);
        out.put("totalPages", totalPages);
        if (p.page() < totalPages) {
            nextQuery.add("page", p.page() + 1).add("pageSize", p.pageSize());
            out.put("next", nextQuery.relative(path));
        } else {
            out.putNull("next");
        }
        return out;
    }

    static ObjectNode fromCat(JsonNode cat, Paging p, Http.Query next, String path, boolean nullScores) {
        List<JsonNode> items = new ArrayList<>();
        cat.path("items").forEach(items::add);
        return page(cat.path("totalCount").asLong(), items, p, next, path, nullScores);
    }

    static Http.Query qualityQuery(Http.Query q, QualityParams qp) {
        if (qp.minimumScore() != null) q.add("minimumScore", java.math.BigDecimal.valueOf(qp.minimumScore()).stripTrailingZeros().toPlainString());
        if (qp.minimumClassification() != null) q.add("minimumClassification", qp.minimumClassification());
        return q;
    }

    static ResponseEntity<String> json(int status, Object body) {
        return ResponseEntity.status(status).header(HttpHeaders.CONTENT_TYPE, "application/json").body(Json.write(body));
    }

    static ResponseEntity<String> json(Object body) {
        return json(200, body);
    }
}

package eu.simpl.catalogue.qma.client;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.qma.QmaProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Calls from qma to its own catalogue-be. 4xx Problems are passed on; anything else is a 500. */
@Component
public class CatClient {
    private static final Logger log = LoggerFactory.getLogger(CatClient.class);

    private final RestClient http;
    private final String base;

    public CatClient(QmaProps props) {
        this.http = Http.client(Duration.ofSeconds(props.getBackendTimeoutSeconds()));
        this.base = props.getCatUrl();
    }

    public JsonNode quick(String bearer, String attributes, String term, QualityParams q, Paging p) {
        URI uri = Http.query().add("searchTerm", term)
                .add("minimumScore", q.minimumScore() == null ? null : plain(q.minimumScore()))
                .add("minimumClassification", q.minimumClassification())
                .add("page", p.page()).add("pageSize", p.pageSize())
                .uri(base + "/selfDescriptions/quickSearch");
        return call("GET", uri, bearer, attributes, null);
    }

    public JsonNode advanced(String bearer, String attributes, JsonNode body, Paging p) {
        URI uri = Http.query().add("page", p.page()).add("pageSize", p.pageSize()).uri(base + "/selfDescriptions/advancedSearch");
        return call("POST", uri, bearer, attributes, Json.write(body));
    }

    /** Stored RD (meta and content) by id, any status, or empty. */
    public Optional<JsonNode> find(String bearer, String id) {
        URI uri = Http.query().add("ids", id).add("statuses", "active,revoked").add("withContent", "true")
                .uri(base + "/self-descriptions");
        JsonNode res = call("GET", uri, bearer, null, null);
        for (JsonNode item : res.path("items")) {
            if (id.equals(item.path("meta").path("id").asText())) return Optional.of(item);
        }
        return Optional.empty();
    }

    /** All RDs (meta and content) provided by a participant, active and revoked. */
    public List<JsonNode> ownedBy(String bearer, String participant) {
        List<JsonNode> out = new ArrayList<>();
        int offset = 0;
        while (true) {
            URI uri = Http.query().add("providedBy", participant).add("statuses", "active,revoked")
                    .add("withContent", "true").add("offset", offset).add("limit", 1000).uri(base + "/self-descriptions");
            JsonNode res = call("GET", uri, bearer, null, null);
            res.path("items").forEach(out::add);
            int total = res.path("totalCount").asInt();
            offset += 1000;
            if (offset >= total) break;
        }
        return out;
    }

    public JsonNode revoke(String bearer, String id) {
        URI uri = URI.create(base + "/self-descriptions/" + Http.seg(id) + "/revoke");
        return call("POST", uri, bearer, null, null);
    }

    private JsonNode call(String method, URI uri, String bearer, String attributes, String body) {
        try {
            RestClient.RequestBodySpec spec = http.method(org.springframework.http.HttpMethod.valueOf(method)).uri(uri)
                    .header("Authorization", bearer)
                    .accept(MediaType.APPLICATION_JSON);
            if (attributes != null) spec.header("User-Attributes", attributes);
            if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
            return spec.exchange((req, res) -> {
                int status = res.getStatusCode().value();
                String text = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                if (status >= 200 && status < 300) {
                    JsonNode n = Json.parseOrNull(text);
                    return n == null ? Json.object() : n;
                }
                if (status >= 400 && status < 500) throw new ApiException(status, Http.problemBody(status, text));
                log.warn("catalogue-be answered {} for {} {}", status, method, uri);
                throw new ApiException(ProblemTypes.INTERNAL, "The catalogue request failed.");
            });
        } catch (RestClientException e) {
            log.warn("catalogue-be call {} {} failed: {}", method, uri, e.getMessage());
            throw new ApiException(ProblemTypes.INTERNAL, "The catalogue request failed.");
        }
    }

    static String plain(Double d) {
        return java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }
}

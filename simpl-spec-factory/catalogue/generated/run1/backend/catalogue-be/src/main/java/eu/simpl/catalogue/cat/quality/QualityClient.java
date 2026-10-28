package eu.simpl.catalogue.cat.quality;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.cat.CatProps;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Client of the Quality Scoring stand-in (D50): score validation and the active MQP. */
@Component
public class QualityClient {
    public static class ScoringException extends RuntimeException {
        public ScoringException(String m, Throwable c) { super(m, c); }
    }

    private final RestClient http = Http.client(Duration.ofSeconds(10));
    private final String base;
    private QualityProfile defaultProfile;
    private Instant defaultExpires = Instant.EPOCH;

    public QualityClient(CatProps props) {
        this.base = props.getQualityStubUrl();
    }

    /** POST /v1/score-validation. Any failure is a ScoringException (D52: 502). */
    public JsonNode score(JsonNode vc) {
        try {
            String body = http.post().uri(base + "/v1/score-validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.write(vc))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ScoringException("Quality scoring answered " + res.getStatusCode().value(), null);
                    })
                    .body(String.class);
            JsonNode n = Json.parseOrNull(body);
            if (n == null) throw new ScoringException("Quality scoring returned no JSON", null);
            return n;
        } catch (RestClientException e) {
            throw new ScoringException("Quality scoring failed: " + e.getMessage(), e);
        }
    }

    /** GET /v1/profiles/active?schemaName=..; empty when the stub has none (404). */
    public Optional<QualityProfile> activeProfile(String schemaName) {
        var q = Http.query().add("schemaName", schemaName);
        try {
            var res = http.get().uri(q.uri(base + "/v1/profiles/active")).retrieve()
                    .onStatus(s -> s.value() == 404, (req, r) -> {})
                    .toEntity(String.class);
            if (res.getStatusCode().value() == 404) return Optional.empty();
            JsonNode n = Json.parseOrNull(res.getBody());
            if (n == null) throw new IllegalStateException("Profile is not JSON");
            return Optional.of(QualityProfile.parse(n));
        } catch (RestClientException e) {
            throw new IllegalStateException("Cannot read the active quality profile: " + e.getMessage(), e);
        }
    }

    /** Default profile, cached for 30 s; used to order labels for minimumClassification (D53). */
    public synchronized QualityProfile defaultProfile() {
        if (defaultProfile == null || Instant.now().isAfter(defaultExpires)) {
            defaultProfile = activeProfile(null).orElseThrow(() -> new IllegalStateException("No default quality profile"));
            defaultExpires = Instant.now().plusSeconds(30);
        }
        return defaultProfile;
    }
}

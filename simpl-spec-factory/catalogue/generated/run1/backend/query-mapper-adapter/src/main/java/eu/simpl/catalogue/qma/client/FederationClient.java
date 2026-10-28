package eu.simpl.catalogue.qma.client;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.TokenClient;
import eu.simpl.catalogue.qma.QmaProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Trusted-side calls to Trustor qma instances (D36, D39, D43, D47, D48): federation token, User-Attributes,
 * a fresh Correlation-Id, 3 s timeout, and the error translation of D39 and D43.
 */
@Component
public class FederationClient {
    private static final Logger log = LoggerFactory.getLogger(FederationClient.class);

    private final RestClient http;
    private final TokenClient tokens;

    public FederationClient(QmaProps props, FederationConfig config) {
        this.http = Http.client(Duration.ofSeconds(props.getFederationTimeoutSeconds()));
        this.tokens = config.outboundClient == null ? null : new TokenClient(config.outboundClient);
    }

    /** For detail calls: 404 and policy-denied 403 are passed on as such. */
    public JsonNode call(FederationConfig.FederatedCatalogue c, HttpMethod method, String pathAndQuery, String body,
                         String attributes, boolean detail) {
        if (!c.active) {
            throw new ApiException(ProblemTypes.FEDERATION_UNAVAILABLE, "Catalogue " + c.id + " is not active.");
        }
        String token;
        try {
            if (tokens == null) throw new IllegalStateException("No outbound federation client configured");
            token = tokens.token();
        } catch (RuntimeException e) {
            log.warn("Cannot get a federation token: {}", e.getMessage());
            throw new ApiException(ProblemTypes.FEDERATION_UNAVAILABLE, "The federation token could not be obtained.");
        }
        URI uri = URI.create(c.authorityURL + pathAndQuery);
        String correlationId = UUID.randomUUID().toString();
        try {
            RestClient.RequestBodySpec spec = http.method(method).uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Attributes", attributes == null ? "" : attributes)
                    .header("Correlation-Id", correlationId)
                    .accept(MediaType.APPLICATION_JSON);
            if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
            return spec.exchange((req, res) -> {
                int status = res.getStatusCode().value();
                String text = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                if (status == 200) {
                    JsonNode n = Json.parseOrNull(text);
                    if (n == null) throw new ApiException(ProblemTypes.FEDERATION_UNAVAILABLE,
                            "Catalogue " + c.id + " returned an unreadable answer.");
                    return n;
                }
                log.info("Trustor {} answered {} for {} (correlation {})", c.id, status, uri, correlationId);
                JsonNode problem = Json.parseOrNull(text);
                String type = problem == null ? "" : problem.path("type").asText("");
                if (detail && status == 404) {
                    throw new ApiException(ProblemTypes.NOT_FOUND, "Resource description was not found in catalogue " + c.id + ".");
                }
                if (detail && status == 403 && type.equals(ProblemTypes.DISCOVERY_POLICY_DENIED.uri)) {
                    throw new ApiException(ProblemTypes.DISCOVERY_POLICY_DENIED,
                            "The discovery access policy of the resource description does not permit this Consumer.");
                }
                if (status == 401 || status == 403) {
                    throw new ApiException(ProblemTypes.FEDERATION_UNAUTHENTICATED,
                            "The Governance Authority of catalogue " + c.id + " did not accept this Governance Authority.");
                }
                if (status >= 500) {
                    throw new ApiException(ProblemTypes.FEDERATION_UNAVAILABLE, "Catalogue " + c.id + " is not available.");
                }
                throw new ApiException(ProblemTypes.INTERNAL, "The request to catalogue " + c.id + " failed.");
            });
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            if (Http.isTimeout(e)) {
                throw new ApiException(ProblemTypes.FEDERATION_TIMEOUT, "Catalogue " + c.id + " did not answer in time.");
            }
            log.info("Trustor {} unreachable: {}", c.id, e.getMessage());
            throw new ApiException(ProblemTypes.FEDERATION_UNAVAILABLE, "Catalogue " + c.id + " is not available.");
        }
    }
}

package eu.simpl.catalogue.cbe;

import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.ProblemTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Raw HTTP calls from cbe to qma, cat and the stubs (10 s timeout, D47). */
@Component
public class Downstream {
    private static final Logger log = LoggerFactory.getLogger(Downstream.class);

    public record Res(int status, String body, String contentType) {
        public boolean ok() { return status >= 200 && status < 300; }
    }

    private final RestClient http = Http.client(Duration.ofSeconds(10));

    public Res call(HttpMethod method, URI uri, String bearer, String body, String accept) {
        try {
            RestClient.RequestBodySpec spec = http.method(method).uri(uri);
            if (bearer != null) spec.header("Authorization", bearer);
            if (accept != null) spec.header("Accept", accept);
            if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
            return spec.exchange((req, res) -> new Res(res.getStatusCode().value(),
                    new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8),
                    res.getHeaders().getContentType() == null ? null : res.getHeaders().getContentType().toString()));
        } catch (RestClientException e) {
            log.warn("Downstream call {} {} failed: {}", method, uri, e.getMessage());
            if (Http.isTimeout(e)) throw new ApiException(ProblemTypes.INTERNAL, 504, "A downstream service did not answer in time.");
            throw new ApiException(ProblemTypes.INTERNAL, "A downstream service is not available.");
        }
    }
}

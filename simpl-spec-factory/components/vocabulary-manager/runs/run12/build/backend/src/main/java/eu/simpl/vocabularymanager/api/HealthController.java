package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.security.TokenVerifier;
import eu.simpl.vocabularymanager.store.StoreClient;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /v1/health, the readiness view (D44, D108, D134, D149, D152): 503 DOWN when the store does
 * not answer within 2 s, 200 DEGRADED when no realm keys could be loaded, else 200 UP. Never
 * fetches keys. Liveness for the container is on the management port.
 */
@RestController
public class HealthController {

    private final StoreClient store;
    private final TokenVerifier tokens;

    public HealthController(StoreClient store, TokenVerifier tokens) {
        this.store = store;
        this.tokens = tokens;
    }

    @GetMapping(path = "/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Dto.HealthDto> health(HttpServletRequest request) {
        QueryParams.parse(request.getQueryString(), RequestRules.NONE);
        if (!store.ping(Duration.ofSeconds(2))) {
            return ResponseEntity.status(503).body(new Dto.HealthDto("DOWN"));
        }
        return ResponseEntity.ok(new Dto.HealthDto(tokens.degraded() ? "DEGRADED" : "UP"));
    }
}

package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.HealthResponse;
import eu.simpl.vocabularymanager.security.RealmKeyCache;
import eu.simpl.vocabularymanager.store.StoreClient;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health, the readiness view of D44, D108, D134 and D152: DOWN (503) when the store does not answer within
 * 2 s; DEGRADED when no realm keys are cached and the last key fetch failed; otherwise UP. Never fetches keys.
 */
@RestController
@RequestMapping("/v1")
public class HealthController {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final StoreClient store;
    private final RealmKeyCache keys;

    public HealthController(StoreClient store, RealmKeyCache keys) {
        this.store = store;
        this.keys = keys;
    }

    /** GET /health. */
    @GetMapping(
            path = "/health",
            produces = {VocabularyController.JSON, VocabularyController.PROBLEM})
    public ResponseEntity<HealthResponse> getHealth(HttpServletRequest request) {
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        if (!store.probe(PROBE_TIMEOUT)) {
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new HealthResponse("DOWN"));
        }
        String status = !keys.hasKeys() && keys.lastFetchFailed() ? "DEGRADED" : "UP";
        return VocabularyController.json(new HealthResponse(status));
    }
}

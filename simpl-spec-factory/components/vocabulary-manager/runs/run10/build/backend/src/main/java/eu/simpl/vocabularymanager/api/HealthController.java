package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.Dtos;
import eu.simpl.vocabularymanager.security.RealmKeyProvider;
import eu.simpl.vocabularymanager.store.SparqlStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (D44, D108): 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when the store answers
 * but no realm keys are cached and they cannot be fetched; otherwise 200 UP.
 */
@RestController
public class HealthController {

    private final SparqlStore store;
    private final RealmKeyProvider keys;

    public HealthController(SparqlStore store, RealmKeyProvider keys) {
        this.store = store;
        this.keys = keys;
    }

    @GetMapping("/v1/health")
    public ResponseEntity<Dtos.HealthResponse> getHealth(HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        checks.throwIfAny();
        if (!store.ping()) {
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Dtos.HealthResponse("DOWN"));
        }
        boolean degraded = !keys.hasKeys() && keys.lastFetchFailed() && !keys.refresh();
        return VocabularyController.json(new Dtos.HealthResponse(degraded ? "DEGRADED" : "UP"));
    }
}

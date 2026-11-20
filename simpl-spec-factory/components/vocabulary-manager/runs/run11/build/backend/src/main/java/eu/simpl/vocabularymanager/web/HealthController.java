package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.security.RealmKeys;
import eu.simpl.vocabularymanager.store.SparqlClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (D44, D108, D134): DOWN (503) when the store does not answer within 2 s, DEGRADED
 * when the store answers but no realm keys are loaded and the last key fetch failed, otherwise UP.
 * Never fetches keys. No token needed (D17).
 */
@RestController
public class HealthController {

    private final SparqlClient store;
    private final RealmKeys keys;

    public HealthController(SparqlClient store, RealmKeys keys) {
        this.store = store;
        this.keys = keys;
    }

    @GetMapping(value = "/v1/health", produces = VocabularyController.JSON)
    public ResponseEntity<Map<String, String>> getHealth(HttpServletRequest request) {
        RequestChecks.start().noQuery(request.getQueryString()).throwIfAny();
        if (!store.ping()) {
            return ResponseEntity.status(503)
                    .header("Content-Type", VocabularyController.JSON)
                    .body(Map.of("status", "DOWN"));
        }
        String status = !keys.loaded() && keys.lastFetchFailed() ? "DEGRADED" : "UP";
        return ResponseEntity.ok()
                .header("Content-Type", VocabularyController.JSON)
                .body(Map.of("status", status));
    }
}

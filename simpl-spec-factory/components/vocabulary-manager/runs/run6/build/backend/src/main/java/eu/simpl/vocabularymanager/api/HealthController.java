package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.security.RealmKeySource;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (decision D44): 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when the store
 * answers but no realm keys are loaded and Keycloak does not answer; otherwise 200 UP. No token needed.
 */
@RestController
public class HealthController {

    private final VocabularyStore store;
    private final RealmKeySource keys;

    /**
     * Creates the controller.
     *
     * @param store the store
     * @param keys the realm key source
     */
    public HealthController(VocabularyStore store, RealmKeySource keys) {
        this.store = store;
        this.keys = keys;
    }

    /**
     * Answers the health probe.
     *
     * @return the health status
     */
    @GetMapping("/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        if (!store.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "DOWN"));
        }
        boolean identity = keys.hasKeys() || keys.refresh();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", identity ? "UP" : "DEGRADED"));
    }
}

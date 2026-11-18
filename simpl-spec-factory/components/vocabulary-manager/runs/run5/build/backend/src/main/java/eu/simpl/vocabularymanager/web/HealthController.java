package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.security.IdentityProviderUnavailableException;
import eu.simpl.vocabularymanager.security.RealmKeys;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (D44, MACH-08): 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when the store answers
 * but no realm keys are loaded and Keycloak does not answer; otherwise 200 UP. Needs no token.
 */
@RestController
public class HealthController {

    private final VocabularyStore store;
    private final RealmKeys keys;
    private final Duration timeout;

    public HealthController(VocabularyStore store, RealmKeys keys, VocabularyProperties properties) {
        this.store = store;
        this.keys = keys;
        this.timeout = properties.store().healthTimeout();
    }

    @GetMapping(path = "/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> health() {
        if (!store.isAvailable(timeout)) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
        if (!keys.hasKeys()) {
            try {
                keys.refresh();
            } catch (IdentityProviderUnavailableException e) {
                return ResponseEntity.ok(Map.of("status", "DEGRADED"));
            }
        }
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

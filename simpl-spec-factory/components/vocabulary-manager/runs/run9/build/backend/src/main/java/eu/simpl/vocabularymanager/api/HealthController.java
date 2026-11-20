package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.security.RealmKeySource;
import eu.simpl.vocabularymanager.store.SparqlStore;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /health without a token (D44): DOWN, DEGRADED or UP. */
@RestController
public class HealthController {

    private final SparqlStore store;
    private final RealmKeySource keys;
    private final VocabularyManagerProperties.Store config;

    /**
     * @param store the store
     * @param keys realm keys
     * @param properties component configuration
     */
    public HealthController(SparqlStore store, RealmKeySource keys, VocabularyManagerProperties properties) {
        this.store = store;
        this.keys = keys;
        this.config = properties.store();
    }

    /** GET /health. */
    @GetMapping("/v1/health")
    public ResponseEntity<byte[]> health(HttpServletRequest request) {
        RequestChecks.acceptJson(request);
        RequestChecks.query(request, Set.of());
        if (!store.ping(config.healthTimeout())) {
            return JsonResponses.json(503, Map.of("status", "DOWN"));
        }
        if (!keys.hasKeys() && !keys.refresh()) {
            return JsonResponses.json(200, Map.of("status", "DEGRADED"));
        }
        return JsonResponses.json(200, Map.of("status", "UP"));
    }
}

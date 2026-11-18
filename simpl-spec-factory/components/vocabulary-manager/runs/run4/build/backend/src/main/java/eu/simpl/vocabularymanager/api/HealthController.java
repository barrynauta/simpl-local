package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.ApiModel.HealthResponse;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.security.RealmKeys;
import eu.simpl.vocabularymanager.store.SparqlClient;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (decision D44): DOWN (503) when the store does not answer within 2 s; DEGRADED when the store answers
 * but no realm keys are loaded and Keycloak does not answer; otherwise UP. No token needed.
 */
@RestController
public class HealthController {

    private final SparqlClient sparql;
    private final RealmKeys realmKeys;
    private final Duration storeTimeout;

    public HealthController(SparqlClient sparql, RealmKeys realmKeys, VocabularyManagerProperties properties) {
        this.sparql = sparql;
        this.realmKeys = realmKeys;
        this.storeTimeout = properties.store().healthTimeout();
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponse> health(HttpServletRequest request) {
        QueryParameters.read(request, Set.of());
        if (!sparql.ping(storeTimeout)) {
            return ResponseEntity.status(503).body(new HealthResponse("DOWN"));
        }
        if (!realmKeys.hasKeys() && !realmKeys.refreshQuietly()) {
            return ResponseEntity.ok(new HealthResponse("DEGRADED"));
        }
        return ResponseEntity.ok(new HealthResponse("UP"));
    }
}

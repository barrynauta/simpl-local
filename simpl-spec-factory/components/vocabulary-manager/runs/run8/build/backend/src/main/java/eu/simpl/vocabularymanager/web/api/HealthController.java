package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.security.IdentityProviderUnavailableException;
import eu.simpl.vocabularymanager.security.RealmKeySource;
import eu.simpl.vocabularymanager.store.SparqlStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (D44): 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when the store answers but no
 * realm keys are loaded and the identity provider does not answer; otherwise 200 UP. No token needed.
 */
@RestController
public class HealthController {

    private final SparqlStore store;
    private final RealmKeySource keySource;
    private final VocabularyManagerProperties properties;
    private final Responses responses;

    /**
     * @param store the store
     * @param keySource the realm keys
     * @param properties component settings
     * @param responses response builder
     */
    public HealthController(
            SparqlStore store, RealmKeySource keySource, VocabularyManagerProperties properties, Responses responses) {
        this.store = store;
        this.keySource = keySource;
        this.properties = properties;
        this.responses = responses;
    }

    /**
     * getHealth.
     *
     * @return UP, DEGRADED or DOWN
     */
    @GetMapping("/health")
    public ResponseEntity<byte[]> getHealth() {
        if (!store.ping(properties.store().healthTimeout())) {
            ResponseEntity<byte[]> down = responses.ok(new Representations.HealthResponse("DOWN"));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(down.getHeaders())
                    .body(down.getBody());
        }
        if (!keySource.hasKeys()) {
            try {
                keySource.refresh();
            } catch (IdentityProviderUnavailableException e) {
                return responses.ok(new Representations.HealthResponse("DEGRADED"));
            }
        }
        return responses.ok(new Representations.HealthResponse("UP"));
    }
}

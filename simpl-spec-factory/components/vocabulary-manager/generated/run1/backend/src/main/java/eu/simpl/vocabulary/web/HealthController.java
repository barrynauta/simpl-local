package eu.simpl.vocabulary.web;

import eu.simpl.vocabulary.store.JenaStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public health endpoint (OpenAPI getHealth, D2). UP when the RDF store answers, DOWN (503) otherwise. */
@RestController
public class HealthController {

    private final JenaStore store;

    public HealthController(JenaStore store) {
        this.store = store;
    }

    @GetMapping(path = "/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Dtos.HealthResponse> health() {
        return store.ping()
                ? ResponseEntity.ok(new Dtos.HealthResponse("UP"))
                : ResponseEntity.status(503).body(new Dtos.HealthResponse("DOWN"));
    }
}

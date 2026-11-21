package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.HealthResponse;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.security.RealmKeyProvider;
import eu.simpl.vocabularymanager.store.SparqlStoreClient;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health, the readiness view of D44, D108, D134 and D152: DOWN (503) when the store does not answer
 * within 2 s; DEGRADED when the store answers but no realm keys are loaded after a failed fetch; else UP.
 * It never fetches keys.
 */
@RestController
public class HealthController {

    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);
    private static final Query PING = QueryFactory.create("SELECT (1 AS ?ok) WHERE {} LIMIT 1");

    private final SparqlStoreClient store;
    private final RealmKeyProvider keys;
    private final Duration timeout;

    public HealthController(SparqlStoreClient store, RealmKeyProvider keys, VocabularyManagerProperties properties) {
        this.store = store;
        this.keys = keys;
        this.timeout = properties.store().healthTimeout();
    }

    @GetMapping(
            path = "/v1/health",
            produces = {ApiSupport.JSON, ApiSupport.PROBLEM_JSON})
    public ResponseEntity<HealthResponse> getHealth(HttpServletRequest request) {
        List<ProblemIssue> issues = new ArrayList<>();
        ApiSupport.noQuery(request, issues);
        ApiSupport.failOnIssues(issues);
        try {
            store.selectOnce(PING, timeout);
        } catch (RuntimeException e) {
            LOG.warn("Health: store not available ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new HealthResponse("DOWN"));
        }
        String status = keys.state() == RealmKeyProvider.KeyState.FETCH_FAILED ? "DEGRADED" : "UP";
        return ApiSupport.json(new HealthResponse(status));
    }
}

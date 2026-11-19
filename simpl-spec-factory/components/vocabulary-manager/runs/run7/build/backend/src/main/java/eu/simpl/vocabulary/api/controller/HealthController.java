package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.dto.HealthDto;
import eu.simpl.vocabulary.api.request.QueryParameters;
import eu.simpl.vocabulary.security.RealmKeySource;
import eu.simpl.vocabulary.store.VocabularyStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health (D44): 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when no realm keys
 * are loaded and the identity provider does not answer; otherwise 200 UP. Graceful degradation pattern.
 */
@RestController
@RequestMapping("/v1")
public class HealthController {

    private final VocabularyStore store;
    private final RealmKeySource keys;

    public HealthController(VocabularyStore store, RealmKeySource keys) {
        this.store = store;
        this.keys = keys;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthDto> health(HttpServletRequest request) {
        Requests.rejectIfAny(QueryParameters.parse(request.getQueryString()).requireNone());
        if (!store.ping()) {
            return Requests.json(503, new HealthDto("DOWN"));
        }
        if (!keys.hasKeys()) {
            keys.tryRefresh();
            if (!keys.hasKeys()) {
                return Requests.json(200, new HealthDto("DEGRADED"));
            }
        }
        return Requests.json(200, new HealthDto("UP"));
    }
}

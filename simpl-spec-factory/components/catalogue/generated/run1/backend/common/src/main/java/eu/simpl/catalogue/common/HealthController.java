package eu.simpl.catalogue.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** D3: every service answers GET /health without authentication. */
@RestController
public class HealthController {
    @GetMapping(value = "/health", produces = "application/json")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

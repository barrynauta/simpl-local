package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.Problems;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** D13 health. */
@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public HealthController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @GetMapping("/health")
    public void health(HttpServletResponse res) throws IOException {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            Problems.write(res, 503, Problems.INTERNAL_SERVER_ERROR, "Service Unavailable", "/contract-ui/v1/health");
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("groups", List.of("liveness", "readiness"));
        body.put("components", Map.of("db", Map.of("status", "UP"), "ping", Map.of("status", "UP")));
        res.setStatus(200);
        res.setContentType("application/vnd.spring-boot.actuator.v3+json");
        mapper.writeValue(res.getOutputStream(), body);
    }
}

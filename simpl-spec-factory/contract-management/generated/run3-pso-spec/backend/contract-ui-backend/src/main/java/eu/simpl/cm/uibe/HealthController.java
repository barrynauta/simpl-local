package eu.simpl.cm.uibe;

import eu.simpl.cm.common.ProblemTypes;
import eu.simpl.cm.common.ProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** GET /health stays open on UIBE (D13). */
@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public void health(HttpServletRequest req, HttpServletResponse res) throws IOException {
        boolean db;
        try {
            Integer one = jdbc.queryForObject("select 1", Integer.class);
            db = one != null && one == 1;
        } catch (Exception e) {
            db = false;
        }
        if (!db) {
            ProblemWriter.write(req, res, 503, ProblemTypes.INTERNAL_SERVER_ERROR, "Service Unavailable",
                    "Service Unavailable");
            return;
        }
        byte[] body = "{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"],\"components\":{\"ping\":{\"status\":\"UP\"}}}"
                .getBytes(StandardCharsets.UTF_8);
        res.setStatus(200);
        res.setContentType("application/vnd.spring-boot.actuator.v3+json");
        res.setContentLength(body.length);
        res.getOutputStream().write(body);
    }
}

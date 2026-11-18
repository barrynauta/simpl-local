package eu.simpl.vocabularymanager.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Turns an {@link ApiException} into an application/problem+json response. */
@Component
public class ProblemWriter {

    private final ObjectMapper mapper;

    public ProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** For filters: writes the problem straight to the servlet response. */
    public void write(HttpServletResponse response, ApiException e) throws IOException {
        response.setStatus(e.problem().status());
        e.headers().forEach(response::setHeader);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), e.problem());
    }

    /** For controllers and the exception handler. */
    public ResponseEntity<Problem> entity(ApiException e) {
        HttpHeaders headers = new HttpHeaders();
        e.headers().forEach(headers::set);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return ResponseEntity.status(e.problem().status()).headers(headers).body(e.problem());
    }
}

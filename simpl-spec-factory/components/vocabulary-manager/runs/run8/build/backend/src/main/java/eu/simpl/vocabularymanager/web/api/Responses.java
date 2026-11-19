package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.web.problem.Problems;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds success responses with a preset Content-Type, so the body format never depends on content negotiation (the
 * Accept check of D63 has already run).
 */
@Component
public class Responses {

    /** text/turtle without charset (D70). */
    public static final MediaType TURTLE = new MediaType("text", "turtle");

    private final JsonMapper jsonMapper;

    /**
     * @param jsonMapper the JSON mapper
     */
    public Responses(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * @param body the representation
     * @return 200 with application/json
     */
    public ResponseEntity<byte[]> ok(Object body) {
        return json(HttpStatus.OK, body, null);
    }

    /**
     * @param body the representation
     * @param location the Location header, relative to the API base
     * @return 201 with application/json
     */
    public ResponseEntity<byte[]> created(Object body, String location) {
        return json(HttpStatus.CREATED, body, location);
    }

    /**
     * @param content stored Turtle bytes
     * @param filename the attachment filename (D29)
     * @return 200 with text/turtle
     */
    public ResponseEntity<byte[]> turtle(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TURTLE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> json(HttpStatus status, Object body, String location) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (location != null) {
            headers.set(HttpHeaders.LOCATION, location);
        }
        return new ResponseEntity<>(jsonMapper.writeValueAsBytes(body), headers, status);
    }

    /**
     * @return the caller of the current request
     */
    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new IllegalStateException("No authenticated user on a secured operation");
    }

    /**
     * @param issues collected issues
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 400 when there are issues
     */
    public static void failOn(List<eu.simpl.vocabularymanager.web.problem.ProblemIssue> issues) {
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
    }
}

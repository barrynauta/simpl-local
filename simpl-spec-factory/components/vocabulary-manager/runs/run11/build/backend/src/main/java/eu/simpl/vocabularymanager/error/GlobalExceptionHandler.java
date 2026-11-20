package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * One handler for every exception (CODE-34, CODE-36, D104): the problem type gives status, type
 * and title; anything unexpected is a sanitised 500 (BLG-err-sanit) and is logged with its stack
 * trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemWriter problems;

    public GlobalExceptionHandler(ProblemWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex) {
        if (ex.problemType().status() >= 500) {
            LOG.warn("Request failed: {}", ex.getMessage());
        } else if (ex.problemType() == ProblemType.BAD_REQUEST) {
            LOG.info("Input validation failed: {} issues", ex.issues().size());
        }
        return respond(ex);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> noHandler(Exception ex) {
        return respond(new ApiException(ProblemType.NOT_FOUND, "No resource exists at this path."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method(HttpRequestMethodNotSupportedException ex) {
        String allow = ex.getSupportedHttpMethods() == null
                ? ""
                : String.join(
                        ", ",
                        ex.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .toList());
        return respond(new ApiException(
                ProblemType.METHOD_NOT_ALLOWED,
                "Method not allowed on this resource.",
                List.of(),
                Map.of("Allow", allow),
                false));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> notAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return respond(new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> unsupported(HttpMediaTypeNotSupportedException ex) {
        return respond(new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> multipart(MultipartException ex) {
        return respond(eu.simpl.vocabularymanager.web.MultipartForm.translate(ex));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest request) {
        LOG.error("Unexpected failure", ex);
        return respond(new ApiException(ProblemType.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    }

    private ResponseEntity<Map<String, Object>> respond(ApiException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ProblemWriter.PROBLEM_JSON));
        ex.headers().forEach(headers::set);
        return new ResponseEntity<>(
                problems.body(ex), headers, HttpStatus.valueOf(ex.problemType().status()));
    }
}

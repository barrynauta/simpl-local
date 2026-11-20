package eu.simpl.vocabularymanager.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The one handler that maps exceptions to application/problem+json (D104, CODE-34 to CODE-36). Problem exceptions
 * carry their own status; framework exceptions get a defined problem; anything else is a logged 500 without
 * internals in the body (BLG-err-sanit).
 */
@RestControllerAdvice
public class ProblemHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType(ProblemWriter.PROBLEM_JSON);

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ProblemBody> problem(ProblemException problem) {
        if (problem.status() >= 500) {
            LOG.warn("Request failed: status={} detail={}", problem.status(), problem.detail());
        } else {
            LOG.info("Request rejected: status={} type={}", problem.status(), problem.type());
        }
        return toResponse(problem);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemBody> tooLarge(MaxUploadSizeExceededException e) {
        return toResponse(Problems.uploadTooLarge());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemBody> multipart(MultipartException e) {
        return toResponse(Problems.badRequest(
                Issue.schemaViolation("body", "body", "The multipart request body could not be read.", null)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemBody> unsupported(HttpMediaTypeNotSupportedException e) {
        return toResponse(Problems.unsupportedMediaType());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemBody> notAcceptable(HttpMediaTypeNotAcceptableException e) {
        return toResponse(Problems.notAcceptable());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemBody> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(Problems.methodNotAllowed(null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemBody> notFound(NoResourceFoundException e) {
        return toResponse(Problems.pathNotFound());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemBody> unexpected(Exception e) {
        LOG.error("Unexpected error", e);
        return toResponse(Problems.internalError());
    }

    static ResponseEntity<ProblemBody> toResponse(ProblemException problem) {
        HttpHeaders headers = new HttpHeaders();
        problem.headers().forEach(headers::set);
        return ResponseEntity.status(problem.status())
                .headers(headers)
                .contentType(PROBLEM_JSON)
                .body(ProblemBody.of(problem));
    }
}

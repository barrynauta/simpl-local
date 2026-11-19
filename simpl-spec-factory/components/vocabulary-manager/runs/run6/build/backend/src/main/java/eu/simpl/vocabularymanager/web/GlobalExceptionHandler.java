package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.problem.Problem;
import eu.simpl.vocabularymanager.problem.ProblemException;
import eu.simpl.vocabularymanager.problem.ProblemWriter;
import eu.simpl.vocabularymanager.problem.Problems;
import eu.simpl.vocabularymanager.store.StoreFailureException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * One exception strategy for the whole component (CODE-34 to CODE-36): every exception maps to a defined problem;
 * unexpected ones give 500 without internals and are logged with their stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemWriter problems;

    /**
     * Creates the handler.
     *
     * @param problems the problem writer
     */
    public GlobalExceptionHandler(ProblemWriter problems) {
        this.problems = problems;
    }

    /**
     * Expected failures with a defined problem.
     *
     * @param e the exception
     * @return the problem response
     */
    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<Problem> problem(ProblemException e) {
        if (e.status() >= 500) {
            LOG.warn("Request failed with {}: {}", e.status(), e.problem().detail());
        } else {
            LOG.info("Request rejected with {}: {}", e.status(), e.problem().title());
        }
        return problems.entity(e.problem(), e.headers());
    }

    /**
     * The store is unreachable or timed out (decision D44).
     *
     * @param e the exception
     * @return 503 with Retry-After
     */
    @ExceptionHandler(StoreUnavailableException.class)
    public ResponseEntity<Problem> storeUnavailable(StoreUnavailableException e) {
        LOG.warn("Store unavailable: {}", e.getMessage());
        ProblemException problem = Problems.serviceUnavailable(Problems.STORE_DOWN_DETAIL);
        return problems.entity(problem.problem(), problem.headers());
    }

    /**
     * The store answered with an error (decision D44: stays 500).
     *
     * @param e the exception
     * @return 500
     */
    @ExceptionHandler(StoreFailureException.class)
    public ResponseEntity<Problem> storeFailure(StoreFailureException e) {
        LOG.error("Store failure", e);
        return problems.entity(Problems.internalError(), Map.of());
    }

    /**
     * Upload above the multipart limits (decision D15).
     *
     * @param e the exception
     * @return 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Problem> tooLarge(MaxUploadSizeExceededException e) {
        LOG.info("Upload too large: {}", e.getMessage());
        return problems.entity(Problems.payloadTooLarge(ContentValidator.FIELD).problem(), Map.of());
    }

    /**
     * A multipart body that cannot be read.
     *
     * @param e the exception
     * @return 400, or 413 when the cause is a size limit
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Problem> badMultipart(MultipartException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName();
            if (name.contains("SizeLimitExceeded") || name.contains("SizeException")) {
                return tooLarge(new MaxUploadSizeExceededException(-1, e));
            }
        }
        LOG.info("Unreadable multipart body: {}", e.getMessage());
        return problems.entity(
                Problems.badRequest(Issue.schemaViolation(
                                Issue.IN_BODY, "body", "The multipart/form-data body cannot be read."))
                        .problem(),
                Map.of());
    }

    /**
     * Unsupported request media type.
     *
     * @param e the exception
     * @return 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Problem> unsupported(HttpMediaTypeNotSupportedException e) {
        return problems.entity(Problems.unsupportedMediaType().problem(), Map.of());
    }

    /**
     * Response media type that cannot be served.
     *
     * @param e the exception
     * @return 406
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Problem> notAcceptable(HttpMediaTypeNotAcceptableException e) {
        return problems.entity(Problems.notAcceptable().problem(), Map.of());
    }

    /**
     * Method not declared for the path.
     *
     * @param e the exception
     * @return 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problem> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        String allow = e.getSupportedHttpMethods() == null
                ? ""
                : String.join(
                        ", ",
                        e.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .toList());
        ProblemException problem = Problems.methodNotAllowed(allow);
        return problems.entity(problem.problem(), problem.headers());
    }

    /**
     * Path outside the API.
     *
     * @param e the exception
     * @return 404
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Problem> unknownPath(Exception e) {
        return problems.entity(Problems.unknownPath(), Map.of());
    }

    /**
     * Anything else: 500 without internals, logged with the stack trace (CODE-29).
     *
     * @param e the exception
     * @return 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> unexpected(Exception e) {
        LOG.error("Unexpected error", e);
        return problems.entity(Problems.internalError(), Map.of());
    }
}

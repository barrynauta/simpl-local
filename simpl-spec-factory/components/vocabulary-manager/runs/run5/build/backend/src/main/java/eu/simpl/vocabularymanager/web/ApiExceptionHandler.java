package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.StoreException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import eu.simpl.vocabularymanager.validation.ContentValidationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * One place that turns exceptions into problem responses (CODE-34 to CODE-36). Internal details are logged, never
 * returned (BLG-err-sanit).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemWriter writer;

    public ApiExceptionHandler(ProblemWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Problem> api(ApiException e) {
        if (e.problem().status() >= 500) {
            LOG.error("Request failed: {}", e.getMessage());
        } else {
            LOG.info(
                    "Request rejected: {} {}", e.problem().status(), e.problem().detail());
        }
        return writer.entity(e);
    }

    /** Validation errors: the validation report (D9, D10, D14). */
    @ExceptionHandler(ContentValidationException.class)
    public ResponseEntity<Problem> validation(ContentValidationException e) {
        LOG.info(
                "Vocabulary content rejected with {} validation error(s)",
                e.errors().size());
        List<Issue> issues = e.errors().stream()
                .map(error -> Issue.invalidInput("vocabularyFile", error.detail(), error.location()))
                .toList();
        return writer.entity(ApiException.badRequest(issues));
    }

    @ExceptionHandler(StoreUnavailableException.class)
    public ResponseEntity<Problem> storeUnavailable(StoreUnavailableException e) {
        LOG.error("Store not available", e);
        return writer.entity(ApiException.serviceUnavailable(VocabularyService.STORE_UNAVAILABLE));
    }

    @ExceptionHandler(StoreException.class)
    public ResponseEntity<Problem> storeError(StoreException e) {
        LOG.error("Store error", e);
        return writer.entity(ApiException.internal());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Problem> noHandler(Exception e) {
        return writer.entity(ApiException.noSuchResource());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problem> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        String allow = e.getSupportedHttpMethods() == null
                ? null
                : String.join(
                        ", ",
                        e.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .toList());
        return writer.entity(ApiException.methodNotAllowed(allow));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Problem> notAcceptable(HttpMediaTypeNotAcceptableException e) {
        return writer.entity(ApiException.notAcceptable());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> unexpected(Exception e) {
        LOG.error("Unexpected error", e);
        return writer.entity(ApiException.internal());
    }
}

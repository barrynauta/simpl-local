package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.ConflictException;
import eu.simpl.vocabularymanager.domain.ContentValidationException;
import eu.simpl.vocabularymanager.domain.ExternalVocabularyNotFoundException;
import eu.simpl.vocabularymanager.domain.ServiceBusyException;
import eu.simpl.vocabularymanager.domain.ValidationIssue;
import eu.simpl.vocabularymanager.domain.VersionNotFoundException;
import eu.simpl.vocabularymanager.domain.VocabularyNotFoundException;
import eu.simpl.vocabularymanager.store.StoreFailureException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import java.util.List;
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
 * One exception strategy for the whole component (CODE-34 to CODE-36): every exception has a decided problem
 * response; internals never reach the client (BLG-err-sanit). Catch order is most specific first.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Decided problem responses raised by the API code. */
    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<byte[]> problem(ProblemException ex) {
        if (ex.problem().status() == 400) {
            LOG.info("Request rejected: {} issues", ex.problem().issues().size());
        }
        return ProblemWriter.entity(ex.problem());
    }

    /** Validation errors of uploaded content (D10). */
    @ExceptionHandler(ContentValidationException.class)
    public ResponseEntity<byte[]> contentInvalid(ContentValidationException ex) {
        LOG.info(
                "Vocabulary content rejected: {} validation errors", ex.issues().size());
        List<Issue> issues =
                ex.issues().stream().map(ApiExceptionHandler::issue).toList();
        return ProblemWriter.entity(Problems.badRequest(issues));
    }

    private static Issue issue(ValidationIssue issue) {
        return Issue.invalidInput("vocabularyFile", issue.detail(), issue.location());
    }

    /** 404 of D24 and D66. */
    @ExceptionHandler(VocabularyNotFoundException.class)
    public ResponseEntity<byte[]> vocabularyNotFound(VocabularyNotFoundException ex) {
        return ProblemWriter.entity(Problems.notFound(ex.getMessage(), "path", "vocabularyName", ex.name()));
    }

    /** 404 of D55. */
    @ExceptionHandler(VersionNotFoundException.class)
    public ResponseEntity<byte[]> versionNotFound(VersionNotFoundException ex) {
        return ProblemWriter.entity(Problems.notFound(ex.getMessage(), "path", "version", null));
    }

    /** 404 of D41 and D66. */
    @ExceptionHandler(ExternalVocabularyNotFoundException.class)
    public ResponseEntity<byte[]> externalNotFound(ExternalVocabularyNotFoundException ex) {
        return ProblemWriter.entity(Problems.notFound(ex.getMessage(), ex.issueIn(), ex.issueName(), null));
    }

    /** 409. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<byte[]> conflict(ConflictException ex) {
        LOG.info("Conflict: {}", ex.getMessage());
        return ProblemWriter.entity(Problems.conflict(ex.getMessage()));
    }

    /** 503 with Retry-After (D43, D44, D92). */
    @ExceptionHandler(ServiceBusyException.class)
    public ResponseEntity<byte[]> busy(ServiceBusyException ex) {
        LOG.warn("Service busy: {}", ex.getMessage());
        return ProblemWriter.entity(Problems.serviceUnavailable(ex.getMessage()));
    }

    /** Store down or slow: 503 (D44). */
    @ExceptionHandler(StoreUnavailableException.class)
    public ResponseEntity<byte[]> storeUnavailable(StoreUnavailableException ex) {
        LOG.error("Store unavailable", ex);
        return ProblemWriter.entity(Problems.serviceUnavailable(ServiceBusyException.STORE_DETAIL));
    }

    /** Store error that is not a timeout or a connection failure: 500 (D44). */
    @ExceptionHandler(StoreFailureException.class)
    public ResponseEntity<byte[]> storeFailure(StoreFailureException ex) {
        LOG.error("Store failure", ex);
        return ProblemWriter.entity(Problems.internalError());
    }

    /** Request size limits of the container (D76). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<byte[]> tooLarge(MaxUploadSizeExceededException ex) {
        return ProblemWriter.entity(Problems.payloadTooLarge());
    }

    /** A malformed multipart request. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<byte[]> multipart(MultipartException ex) {
        return ProblemWriter.entity(Problems.badRequest(
                List.of(Issue.schemaViolation("body", "body", "The multipart request is malformed.", null))));
    }

    /** No handler: 404 (D67). */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<byte[]> noHandler(Exception ex) {
        return ProblemWriter.entity(Problems.unknownResource());
    }

    /** Method not declared: 405 (normally answered by the route filter first). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<byte[]> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ProblemWriter.entity(Problems.methodNotAllowed(null));
    }

    /** Media type not supported: 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<byte[]> unsupported(HttpMediaTypeNotSupportedException ex) {
        return ProblemWriter.entity(Problems.unsupportedMediaType());
    }

    /** Not acceptable: 406. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<byte[]> notAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return ProblemWriter.entity(Problems.notAcceptable());
    }

    /** Anything else: 500 without internals; logged with its stack trace (CODE-29). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> unexpected(Exception ex) {
        LOG.error("Unexpected error", ex);
        return ProblemWriter.entity(Problems.internalError());
    }
}

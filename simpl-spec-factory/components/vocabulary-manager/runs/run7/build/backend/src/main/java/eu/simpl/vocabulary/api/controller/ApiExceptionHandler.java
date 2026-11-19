package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.problem.ApiException;
import eu.simpl.vocabulary.api.problem.Problem;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.store.StoreException;
import eu.simpl.vocabulary.store.StoreUnavailableException;
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
 * One exception strategy for the whole component (CODE-34 to CODE-36): application exceptions carry their
 * problem; store failures become 503 or 500 (D44); framework exceptions map to their status; anything else
 * is logged with its stack trace and answered with a sanitised 500 (BLG-err-sanit).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemWriter problemWriter;

    public ApiExceptionHandler(ProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Problem> handleApi(ApiException e) {
        if (e.status() >= 500) {
            LOG.warn("Request failed: {} {}", e.status(), e.detail());
        }
        return problemWriter.entity(e);
    }

    @ExceptionHandler(StoreUnavailableException.class)
    public ResponseEntity<Problem> handleStoreUnavailable(StoreUnavailableException e) {
        LOG.warn(
                "Store unavailable: {} ({})",
                e.getMessage(),
                e.getCause() == null ? "" : e.getCause().toString());
        return problemWriter.entity(Problems.storeUnavailable());
    }

    @ExceptionHandler(StoreException.class)
    public ResponseEntity<Problem> handleStore(StoreException e) {
        LOG.error("Store error", e);
        return problemWriter.entity(Problems.internalError());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Problem> handleNotFound(Exception e) {
        return problemWriter.entity(Problems.pathNotFound());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problem> handleMethod(HttpRequestMethodNotSupportedException e) {
        String[] supported = e.getSupportedMethods();
        return problemWriter.entity(Problems.methodNotAllowed(supported == null ? null : String.join(", ", supported)));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Problem> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return problemWriter.entity(Problems.notAcceptable());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Problem> handleUnsupported(HttpMediaTypeNotSupportedException e) {
        return problemWriter.entity(Problems.unsupportedMediaType());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Problem> handleTooLarge(MaxUploadSizeExceededException e) {
        return problemWriter.entity(Problems.payloadTooLarge());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Problem> handleMultipart(MultipartException e) {
        LOG.info("Multipart request rejected: {}", e.getMessage());
        return problemWriter.entity(Problems.badRequest(java.util.List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnexpected(Exception e) {
        LOG.error("Unexpected error", e);
        return problemWriter.entity(Problems.internalError());
    }
}

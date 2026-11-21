package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.SparqlStoreClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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
 * The one exception handler (D104, CODE-34, CODE-36): every exception becomes an RFC 9457 problem with
 * the decided type. Unexpected exceptions give 500 without internals (BLG-err-sanit) and are logged with
 * their stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<byte[]> handleApi(ApiException exception) {
        if (exception.problemType().status() >= 500) {
            LOG.warn("Request failed: {}", exception.detail());
        } else {
            LOG.info(
                    "Request rejected: {} {}",
                    exception.problemType().status(),
                    exception.problemType().title());
        }
        return problem(exception);
    }

    @ExceptionHandler(SparqlStoreClient.StoreUnavailableException.class)
    public ResponseEntity<byte[]> handleStoreUnavailable(SparqlStoreClient.StoreUnavailableException exception) {
        LOG.warn("Store not available: {}", exception.getMessage());
        return problem(Problems.storeUnavailable());
    }

    @ExceptionHandler(SparqlStoreClient.StoreErrorException.class)
    public ResponseEntity<byte[]> handleStoreError(SparqlStoreClient.StoreErrorException exception) {
        LOG.error("Store error", exception);
        return problem(Problems.internal());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<byte[]> handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(Problems.unsupportedMediaType());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<byte[]> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return problem(new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<byte[]> handleMethod(HttpRequestMethodNotSupportedException exception) {
        String allow = exception.getSupportedHttpMethods() == null
                ? ""
                : String.join(
                        ", ",
                        exception.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .toList());
        return problem(new ApiException(
                ProblemType.METHOD_NOT_ALLOWED,
                "The method is not allowed on this resource.",
                List.of(),
                Map.of(HttpHeaders.ALLOW, allow),
                false));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<byte[]> handleNotFound(Exception exception) {
        return problem(notFound());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<byte[]> handleUploadSize(MaxUploadSizeExceededException exception) {
        return problem(Problems.fileTooLarge());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<byte[]> handleMultipart(MultipartException exception) {
        return problem(new ApiException(ProblemType.BAD_REQUEST, "The multipart request body could not be read."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleUnexpected(Exception exception) {
        LOG.error("Unexpected error", exception);
        return problem(Problems.internal());
    }

    public static ApiException notFound() {
        return new ApiException(ProblemType.NOT_FOUND, "The requested resource does not exist.");
    }

    static ResponseEntity<byte[]> problem(ApiException exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        exception.headers().forEach(headers::set);
        return ResponseEntity.status(
                        HttpStatusCode.valueOf(exception.problemType().status()))
                .headers(headers)
                .body(ProblemWriter.bytes(exception));
    }
}

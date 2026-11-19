package eu.simpl.vocabularymanager.web.problem;

import eu.simpl.vocabularymanager.store.StoreException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * One exception strategy for the whole component (CODE-34 to CODE-36): application exceptions carry their problem;
 * store, framework and unexpected exceptions map to a fixed problem without internal details (BLG-err-sanit,
 * OWT-A10). Each exception is logged once, with its stack trace when it is unexpected.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemWriter writer;

    /**
     * @param writer writes problem bodies
     */
    public ApiExceptionHandler(ProblemWriter writer) {
        this.writer = writer;
    }

    /**
     * @param exception an application exception
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(ApiException.class)
    public void handleApi(ApiException exception, HttpServletResponse response) throws IOException {
        LOG.info("Request answered with {}: {}", exception.problem().status(), exception.problem().title());
        writer.write(response, exception.problem(), exception.headers());
    }

    /**
     * @param exception the store is unreachable or too slow
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(StoreUnavailableException.class)
    public void handleStoreUnavailable(StoreUnavailableException exception, HttpServletResponse response)
            throws IOException {
        LOG.warn("Vocabulary store not available", exception);
        writer.write(
                response,
                Problems.serviceUnavailableProblem(STORE_UNAVAILABLE),
                Map.of("Retry-After", Problems.RETRY_AFTER_SECONDS));
    }

    /**
     * @param exception a store error that is not a timeout or connection failure
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(StoreException.class)
    public void handleStore(StoreException exception, HttpServletResponse response) throws IOException {
        LOG.error("Vocabulary store error", exception);
        writer.write(response, Problems.internalServerError());
    }

    /**
     * @param exception no handler for the path
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public void handleNotFound(Exception exception, HttpServletResponse response) throws IOException {
        writer.write(response, Problems.unknownPath());
    }

    /**
     * @param exception method not supported by the matched handler
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void handleMethod(HttpRequestMethodNotSupportedException exception, HttpServletResponse response)
            throws IOException {
        writer.write(response, Problems.methodNotAllowed(exception.getMethod()));
    }

    /**
     * @param exception framework content negotiation failure
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleNotAcceptable(HttpMediaTypeNotAcceptableException exception, HttpServletResponse response)
            throws IOException {
        writer.write(response, Problems.notAcceptable().problem());
    }

    /**
     * @param exception framework content type failure
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void handleUnsupported(HttpMediaTypeNotSupportedException exception, HttpServletResponse response)
            throws IOException {
        writer.write(response, Problems.unsupportedMediaType().problem());
    }

    /**
     * @param exception upload over the container limits
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void handleTooLarge(MaxUploadSizeExceededException exception, HttpServletResponse response)
            throws IOException {
        writer.write(response, Problems.payloadTooLarge("vocabularyFile").problem());
    }

    /**
     * @param exception malformed multipart body
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(MultipartException.class)
    public void handleMultipart(MultipartException exception, HttpServletResponse response) throws IOException {
        writer.write(
                response,
                Problems.schemaViolation("body", "body", "The multipart request body cannot be read.")
                        .problem());
    }

    /**
     * @param exception anything unexpected
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(Exception.class)
    public void handleUnexpected(Exception exception, HttpServletResponse response) throws IOException {
        LOG.error("Unexpected error", exception);
        writer.write(response, Problems.internalServerError());
    }
}

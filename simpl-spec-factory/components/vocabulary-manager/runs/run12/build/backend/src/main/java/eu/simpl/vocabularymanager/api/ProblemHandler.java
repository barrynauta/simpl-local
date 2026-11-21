package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.store.StoreException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
 * The one handler that maps exceptions to application/problem+json (D104). Store failures map to
 * 503 (unreachable, timeout, open breaker) or 500 (D44). Nothing internal leaks into the body.
 */
@RestControllerAdvice
public class ProblemHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemHandler.class);

    private final ProblemWriter writer;

    public ProblemHandler(ProblemWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(ApiException.class)
    public void api(ApiException ex, HttpServletResponse response) throws IOException {
        if (ex.problemType().status() >= 500) {
            LOG.warn("request failed: {} {}", ex.problemType().status(), ex.getMessage());
        }
        writer.write(response, ex);
    }

    @ExceptionHandler(StoreException.class)
    public void store(StoreException ex, HttpServletResponse response) throws IOException {
        LOG.warn("store failure: {}", ex.getMessage());
        writer.write(
                response,
                ex.unavailable() ? ApiException.unavailable(ApiException.STORE_UNAVAILABLE) : ApiException.internal());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public void notFound(Exception ex, HttpServletResponse response) throws IOException {
        writer.write(response, new ApiException(ProblemType.NOT_FOUND, "The requested resource was not found."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void method(HttpRequestMethodNotSupportedException ex, HttpServletResponse response) throws IOException {
        String allow = ex.getSupportedHttpMethods() == null
                ? ""
                : String.join(
                        ", ",
                        ex.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .toList());
        writer.write(
                response,
                new ApiException(
                        ProblemType.METHOD_NOT_ALLOWED,
                        "The method is not allowed for this resource.",
                        List.of(),
                        Map.of("Allow", allow),
                        false));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpServletResponse response) throws IOException {
        writer.write(response, new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void unsupported(HttpServletResponse response) throws IOException {
        writer.write(
                response,
                new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void tooLarge(HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.fileTooLarge());
    }

    @ExceptionHandler(MultipartException.class)
    public void multipart(HttpServletResponse response) throws IOException {
        writer.write(response, new ApiException(ProblemType.BAD_REQUEST, "The multipart request could not be read."));
    }

    @ExceptionHandler(Exception.class)
    public void other(Exception ex, HttpServletResponse response) throws IOException {
        LOG.error("unexpected failure", ex);
        writer.write(response, ApiException.internal());
    }
}

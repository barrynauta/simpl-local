package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The one exception handler of the component (D104, CODE-34 to CODE-36): every exception becomes problem+json.
 * 4xx outcomes are logged at INFO without stack trace; 5xx at ERROR with the stack trace. Response bodies never
 * carry internal details (BLG-err-sanit).
 */
@RestControllerAdvice
public class ProblemHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemHandler.class);

    /**
     * Renders the component's own exceptions.
     *
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(ApiException.class)
    public void handleApi(ApiException exception, HttpServletResponse response) throws IOException {
        if (exception.type().status() >= 500) {
            LOG.error("Request failed with {}: {}", exception.type().status(), exception.detail(), exception);
        } else {
            LOG.info(
                    "Request rejected with {}: {}",
                    exception.type().status(),
                    exception.type().title());
        }
        ProblemWriter.write(response, exception);
    }

    /**
     * Framework rejections that the gatekeeper normally answers first.
     *
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler({
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class,
        HttpMediaTypeNotSupportedException.class,
        MaxUploadSizeExceededException.class,
        NoResourceFoundException.class
    })
    public void handleFramework(Exception exception, HttpServletResponse response) throws IOException {
        ProblemType type = switch (exception) {
            case HttpRequestMethodNotSupportedException e -> ProblemType.METHOD_NOT_ALLOWED;
            case HttpMediaTypeNotAcceptableException e -> ProblemType.NOT_ACCEPTABLE;
            case HttpMediaTypeNotSupportedException e -> ProblemType.UNSUPPORTED_MEDIA_TYPE;
            case MaxUploadSizeExceededException e -> ProblemType.PAYLOAD_TOO_LARGE;
            default -> ProblemType.NOT_FOUND;
        };
        LOG.info("Request rejected by the framework with {}", type.status());
        ProblemWriter.write(response, new ApiException(type, null, List.of()));
    }

    /**
     * Anything else is a 500 without internal details (CODE-35).
     *
     * @throws IOException when the client connection fails
     */
    @ExceptionHandler(Exception.class)
    public void handleUnexpected(Exception exception, HttpServletResponse response) throws IOException {
        LOG.error("Unexpected error", exception);
        ProblemWriter.write(response, ApiException.internal(exception));
    }
}

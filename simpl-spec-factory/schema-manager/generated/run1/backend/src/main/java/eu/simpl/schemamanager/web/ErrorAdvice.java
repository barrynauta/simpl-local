package eu.simpl.schemamanager.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps every error to problem+json (D8, D63). */
@RestControllerAdvice
public class ErrorAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorAdvice.class);

    private final Problems problems;

    public ErrorAdvice(Problems problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<byte[]> api(ApiException e) {
        if (e.status().is5xxServerError()) {
            LOG.error("Request failed: {}", e.detail());
        }
        return problems.response(e);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<byte[]> notFound(Exception e) {
        return problems.response(HttpStatus.NOT_FOUND, "The requested resource was not found.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<byte[]> method(HttpRequestMethodNotSupportedException e) {
        return problems.response(HttpStatus.METHOD_NOT_ALLOWED,
                "Method " + e.getMethod() + " is not supported for this resource.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<byte[]> unsupported(Exception e) {
        return problems.response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, Problems.UNSUPPORTED_DETAIL, null);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<byte[]> notAcceptable(Exception e) {
        return problems.response(HttpStatus.NOT_ACCEPTABLE, Problems.NOT_ACCEPTABLE_DETAIL, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<byte[]> tooLarge(Exception e) {
        return problems.response(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.", null);
    }

    @ExceptionHandler({MultipartException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<byte[]> badRequest(Exception e) {
        return problems.response(HttpStatus.BAD_REQUEST, "Malformed request: " + e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> other(Exception e) {
        LOG.error("Unexpected error", e);
        return problems.response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
    }
}

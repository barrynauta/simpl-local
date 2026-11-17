package eu.simpl.cm.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * Maps every error to application/problem+json (D11). The body is written directly,
 * so a strict Accept header never produces an empty error body.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public void api(ApiException e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        ProblemWriter.write(req, res, e);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void method(HttpRequestMethodNotSupportedException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 405, ProblemTypes.METHOD_NOT_SUPPORTED, "Method Not Allowed",
                "Method " + e.getMethod() + " is not supported");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 406, ProblemTypes.NOT_ACCEPTABLE, "Not Acceptable",
                "The requested media type is not available");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void unsupported(HttpMediaTypeNotSupportedException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 415, ProblemTypes.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "Content type is not supported");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void tooLarge(MaxUploadSizeExceededException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 413, ProblemTypes.INVALID_DOCUMENT, "Payload Too Large",
                "The document is larger than allowed");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public void missingPart(MissingServletRequestPartException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 400, ProblemTypes.VALIDATION_FAILURE, "Bad Request",
                "Missing part: " + e.getRequestPartName());
    }

    @ExceptionHandler(MultipartException.class)
    public void multipart(MultipartException e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        ProblemWriter.write(req, res, 400, ProblemTypes.VALIDATION_FAILURE, "Bad Request",
                "Invalid multipart request");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void unreadable(HttpMessageNotReadableException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 400, ProblemTypes.VALIDATION_FAILURE, "Bad Request", "Invalid body content");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class})
    public void mismatch(Exception e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        ProblemWriter.write(req, res, 400, ProblemTypes.ARGUMENT_TYPE_MISMATCH, "Bad Request",
                "Invalid request param");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void noResource(NoResourceFoundException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ProblemWriter.write(req, res, 404, ProblemTypes.NOT_FOUND, "Not Found", "No such resource");
    }

    @ExceptionHandler(Exception.class)
    public void other(Exception e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        LOG.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), e);
        ProblemWriter.write(req, res, 500, ProblemTypes.INTERNAL_SERVER_ERROR, "Server Error", "Server Error");
    }
}

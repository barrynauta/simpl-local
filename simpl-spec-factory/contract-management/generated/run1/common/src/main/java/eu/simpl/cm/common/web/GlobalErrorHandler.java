package eu.simpl.cm.common.web;

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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/** Turns every error into application/problem+json (D11). Never leaks stack traces. */
@RestControllerAdvice
public class GlobalErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    public void api(ApiException e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (e.getBody() != null) {
            Problems.write(res, e.getStatus(), e.getBody());
        } else {
            Problems.write(res, e.getStatus(), e.getType(), e.getMessage(), req.getRequestURI());
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public void mismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 400, Problems.ARGUMENT_TYPE_MISMATCH, "Invalid value for " + e.getName(), req.getRequestURI());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
            HandlerMethodValidationException.class})
    public void missingParam(Exception e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Problems.write(res, 400, Problems.ARGUMENT_TYPE_MISMATCH, "Invalid request param", req.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void unreadable(HttpMessageNotReadableException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 400, Problems.VALIDATION_FAILURE, "Invalid body content", req.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public void missingPart(MissingServletRequestPartException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 400, Problems.VALIDATION_FAILURE, "Missing part " + e.getRequestPartName(), req.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void tooLarge(MaxUploadSizeExceededException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 413, Problems.INVALID_DOCUMENT, "Document is too large", req.getRequestURI());
    }

    @ExceptionHandler(MultipartException.class)
    public void multipart(MultipartException e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Problems.write(res, 400, Problems.VALIDATION_FAILURE, "Invalid multipart request", req.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void method(HttpRequestMethodNotSupportedException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        if (e.getSupportedHttpMethods() != null) {
            res.setHeader("Allow", String.join(", ", e.getSupportedHttpMethods().stream().map(Object::toString).toList()));
        }
        Problems.write(res, 405, Problems.METHOD_NOT_SUPPORTED, "Method " + e.getMethod() + " is not supported", req.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void unsupported(HttpMediaTypeNotSupportedException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 415, Problems.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported", req.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        Problems.write(res, 406, Problems.NOT_ACCEPTABLE, "Requested media type is not available", req.getRequestURI());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public void noHandler(Exception e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Problems.write(res, 404, Problems.NOT_FOUND, "No resource at this path", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public void other(Exception e, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (e.getClass().getName().contains("OptimisticLocking")) {
            Problems.write(res, 409, Problems.INVALID_TRANSITION, "The contract was changed concurrently", req.getRequestURI());
            return;
        }
        log.error("Unhandled error on {} {}: {}", req.getMethod(), req.getRequestURI(), e.toString(), e);
        Problems.write(res, 500, Problems.INTERNAL_SERVER_ERROR, "Server Error", req.getRequestURI());
    }
}

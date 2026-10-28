package eu.simpl.catalogue.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps every error of every backend to a Problem (D2). */
@RestControllerAdvice
public class ProblemAdvice {
    private static final Logger log = LoggerFactory.getLogger(ProblemAdvice.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> api(ApiException e) {
        if (e.status() >= 500) log.warn("Problem {} {}: {}", e.status(), e.typeUri(), e.getMessage());
        return Problems.response(e);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> missingParam(MissingServletRequestParameterException e) {
        return Problems.response(ProblemTypes.INVALID_INPUT,
                "Required query parameter '" + e.getParameterName() + "' is missing.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> missingHeader(MissingRequestHeaderException e) {
        return Problems.response(ProblemTypes.BAD_REQUEST, "Required header " + e.getHeaderName() + " is missing.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> typeMismatch(MethodArgumentTypeMismatchException e) {
        return Problems.response(ProblemTypes.INVALID_INPUT, "Parameter '" + e.getName() + "' has an invalid value.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> unreadable(HttpMessageNotReadableException e) {
        return Problems.response(ProblemTypes.BAD_REQUEST, "The request body is missing or not readable.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> unsupported(HttpMediaTypeNotSupportedException e) {
        return Problems.response(ProblemTypes.UNSUPPORTED_MEDIA_TYPE,
                "Server refused to accept the request because the message content format is not supported.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> notAcceptable(HttpMediaTypeNotAcceptableException e) {
        return Problems.response(ProblemTypes.NOT_ACCEPTABLE,
                "Server cannot produce a response matching the list of acceptable values defined in the request.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> method(HttpRequestMethodNotSupportedException e) {
        return Problems.response(ProblemTypes.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not supported here.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> noResource(NoResourceFoundException e) {
        return Problems.response(ProblemTypes.NOT_FOUND, "No such path.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> other(Exception e) {
        log.error("Unexpected error", e);
        return Problems.response(ProblemTypes.INTERNAL, "An unexpected error occurred on the server.");
    }
}

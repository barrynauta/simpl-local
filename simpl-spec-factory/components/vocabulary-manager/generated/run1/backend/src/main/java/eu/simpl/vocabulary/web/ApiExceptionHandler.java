package eu.simpl.vocabulary.web;

import eu.simpl.vocabulary.domain.DomainException;
import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import eu.simpl.vocabulary.problem.ProblemWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps every error to an application/problem+json response (D5). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemWriter writer;
    private final long maxUploadBytes;

    public ApiExceptionHandler(ProblemWriter writer, eu.simpl.vocabulary.config.AppProperties props) {
        this.writer = writer;
        this.maxUploadBytes = props.maxUploadBytes();
    }

    @ExceptionHandler(ApiException.class)
    public void api(ApiException ex, HttpServletResponse response) throws IOException {
        writer.write(response, ex);
    }

    @ExceptionHandler(DomainException.class)
    public void domain(DomainException ex, HttpServletResponse response) throws IOException {
        if (ex.kind() == DomainException.Kind.NOT_FOUND) {
            writer.write(response, ApiException.notFound(ex.getMessage(), null));
        } else {
            writer.write(response, ApiException.conflict(ex.getMessage()));
        }
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void unsupported(HttpMediaTypeNotSupportedException ex, HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.unsupportedMediaType(
                "Request content type is not supported." + (ex.getContentType() == null ? ""
                        : " Received '" + ex.getContentType() + "'."), null));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpServletResponse response) throws IOException {
        writer.write(response, 406, "urn:problem-type:simpl:notAcceptable", null, "Not Acceptable",
                "Requested media type cannot be served.", List.of(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void unreadable(HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.badRequest(ProblemIssue.schemaViolation("body", null, null,
                "The request body is missing or is not valid JSON.")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void tooLarge(HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.payloadTooLarge(maxUploadBytes));
    }

    @ExceptionHandler(MultipartException.class)
    public void multipart(MultipartException ex, HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.badRequest(ProblemIssue.schemaViolation("body", null, null,
                "The multipart request could not be read: " + ex.getMessage())));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public void badParam(Exception ex, HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.badRequest(ProblemIssue.schemaViolation(null, null, null,
                ex.getMessage())));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void method(HttpRequestMethodNotSupportedException ex, HttpServletResponse response) throws IOException {
        writer.write(response, 405, "urn:problem-type:simpl:methodNotAllowed", null, "Method Not Allowed",
                "Method " + ex.getMethod() + " is not supported for this resource.", List.of(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void noResource(HttpServletResponse response) throws IOException {
        writer.write(response, ApiException.notFound("The requested resource does not exist.", null));
    }

    @ExceptionHandler(Exception.class)
    public void internal(Exception ex, HttpServletResponse response) throws IOException {
        LOG.error("Unexpected error", ex);
        writer.write(response, 500, "urn:problem-type:belgif:internalServerError",
                "https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html",
                "Internal Server Error", "Internal Server Error", List.of(), List.of());
    }
}

package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.error.RequestErrorCode;
import eu.simpl.vocabularymanager.error.ResourceErrorCode;
import eu.simpl.vocabularymanager.error.TechnicalErrorCode;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into an RFC 9457 problem; internals never reach the client (BLG-err-sanit, OWT-A02). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ProblemWriter writer;

    public ApiExceptionHandler(ProblemWriter writer) {
        this.writer = writer;
    }

    /**
     * The component's own errors.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(VocabularyManagerException.class)
    public void handle(VocabularyManagerException e, HttpServletResponse response) throws IOException {
        int status = e.code().status();
        if (status >= 500) {
            LOG.error("Request failed: {}", e.getMessage(), e);
        } else {
            LOG.info("Request rejected: status={} detail={}", status, e.detail());
        }
        String detail = status == 500 ? "Internal Server Error" : e.detail();
        writer.write(response, e.code(), detail, e.issues());
    }

    /**
     * Upload above the multipart limit.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void tooLarge(MaxUploadSizeExceededException e, HttpServletResponse response) throws IOException {
        handle(Problems.uploadTooLarge(), response);
    }

    /**
     * A multipart body that cannot be read.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(MultipartException.class)
    public void badMultipart(MultipartException e, HttpServletResponse response) throws IOException {
        LOG.info("Multipart request rejected: {}", e.getMessage());
        writer.write(
                response,
                RequestErrorCode.BAD_REQUEST,
                Problems.INPUT_INCORRECT,
                List.of(Issue.schemaViolation("body", null, null, "The multipart body cannot be read.")));
    }

    /**
     * A JSON body that cannot be read.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void unreadable(HttpMessageNotReadableException e, HttpServletResponse response) throws IOException {
        writer.write(
                response,
                RequestErrorCode.BAD_REQUEST,
                Problems.INPUT_INCORRECT,
                List.of(Issue.schemaViolation("body", null, null, "The request body cannot be read.")));
    }

    /**
     * Content-Type not supported (decision D15).
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void unsupported(HttpMediaTypeNotSupportedException e, HttpServletResponse response) throws IOException {
        writer.write(
                response, RequestErrorCode.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported.", List.of());
    }

    /**
     * Accept header that cannot be served.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void notAcceptable(HttpMediaTypeNotAcceptableException e, HttpServletResponse response) throws IOException {
        writer.write(response, RequestErrorCode.NOT_ACCEPTABLE, "Requested media type cannot be served.", List.of());
    }

    /**
     * Method not supported on the path.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletResponse response)
            throws IOException {
        if (e.getSupportedHttpMethods() != null) {
            response.setHeader(
                    "Allow",
                    String.join(
                            ", ",
                            e.getSupportedHttpMethods().stream()
                                    .map(Object::toString)
                                    .toList()));
        }
        writer.write(
                response,
                RequestErrorCode.METHOD_NOT_ALLOWED,
                "Method " + e.getMethod() + " is not allowed on this resource.",
                List.of());
    }

    /**
     * Unknown path.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void noResource(NoResourceFoundException e, HttpServletResponse response) throws IOException {
        writer.write(response, ResourceErrorCode.NOT_FOUND, "The requested resource was not found.", List.of());
    }

    /**
     * Anything else: 500 with a generic detail.
     *
     * @throws IOException when the response cannot be written
     */
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception e, HttpServletResponse response) throws IOException {
        LOG.error("Unexpected error", e);
        writer.write(response, TechnicalErrorCode.INTERNAL, "Internal Server Error", List.of());
    }
}

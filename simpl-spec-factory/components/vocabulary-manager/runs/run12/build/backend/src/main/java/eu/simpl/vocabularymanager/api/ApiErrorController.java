package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces the framework's error page: whatever the container itself rejects is answered with
 * problem+json too (D158). A direct request to /error is an unknown path (404).
 */
@RestController
public class ApiErrorController implements ErrorController {

    private final ProblemWriter writer;

    public ApiErrorController(ProblemWriter writer) {
        this.writer = writer;
    }

    @RequestMapping("/error")
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer i ? i : 404;
        if (status < 400) {
            status = 404;
        }
        ProblemType type = ProblemType.forStatus(status);
        String detail =
                switch (status) {
                    case 404 -> "The requested resource was not found.";
                    case 413 -> ApiException.FILE_TOO_LARGE;
                    default -> type.title();
                };
        ApiException ex = status == 413 ? ApiException.fileTooLarge() : new ApiException(type, detail);
        writer.write(response, ex);
        if (response.getStatus() != status && status != type.status()) {
            response.setStatus(status);
        }
    }
}

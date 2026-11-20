package eu.simpl.vocabularymanager.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Container error pages (errors raised outside Spring MVC) as problem+json. A direct request to
 * /error is not an operation and gets 404.
 */
@Controller
public class ProblemErrorController implements ErrorController {

    private final ProblemWriter problems;

    public ProblemErrorController(ProblemWriter problems) {
        this.problems = problems;
    }

    @RequestMapping("/error")
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 404;
        problems.write(response, new ApiException(typeFor(status), detailFor(status)));
    }

    static ProblemType typeFor(int status) {
        for (ProblemType type : ProblemType.values()) {
            if (type.status() == status) {
                return type;
            }
        }
        return status >= 500 ? ProblemType.INTERNAL_SERVER_ERROR : ProblemType.BAD_REQUEST;
    }

    private static String detailFor(int status) {
        return switch (status) {
            case 404 -> "No resource exists at this path.";
            case 413 -> "The request is too large.";
            case 500 -> "Internal Server Error";
            default -> typeFor(status).title();
        };
    }
}

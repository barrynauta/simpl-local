package eu.simpl.vocabularymanager.web.problem;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Container error dispatches (errors raised outside the MVC handlers) answered as problem+json. Served only on the
 * ERROR dispatch; a direct request to /error is denied by the default-deny rule (D84).
 */
@Controller
public class ProblemErrorController implements ErrorController {

    public static final String PATH = "/error";

    private final ProblemWriter writer;

    /**
     * @param writer writes problem bodies
     */
    public ProblemErrorController(ProblemWriter writer) {
        this.writer = writer;
    }

    /**
     * @param request the error dispatch
     * @param response the response
     * @throws IOException when the client connection fails
     */
    @RequestMapping(PATH)
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 500;
        Problem problem = switch (status) {
            case 400 -> Problems.badRequestProblem(null);
            case 404 -> Problems.unknownPath();
            case 405 -> Problems.methodNotAllowed(request.getMethod());
            case 413 -> Problems.payloadTooLarge("vocabularyFile").problem();
            default -> Problems.internalServerError();
        };
        writer.write(response, problem);
    }
}

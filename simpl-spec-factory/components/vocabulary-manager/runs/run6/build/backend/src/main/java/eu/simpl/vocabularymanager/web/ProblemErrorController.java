package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.problem.Problem;
import eu.simpl.vocabularymanager.problem.ProblemWriter;
import eu.simpl.vocabularymanager.problem.Problems;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces the default error page: errors that leave the servlet (container errors, errors in filters) are
 * answered with a problem body that reveals no internals (OWT-A02, BLG-err-sanit).
 */
@RestController
public class ProblemErrorController implements ErrorController {

    private final ProblemWriter problems;

    /**
     * Creates the controller.
     *
     * @param problems the problem writer
     */
    public ProblemErrorController(ProblemWriter problems) {
        this.problems = problems;
    }

    /**
     * Answers an error dispatch.
     *
     * @param request the request
     * @return the problem response
     */
    @RequestMapping("/error")
    public ResponseEntity<Problem> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer i ? i : 500;
        return problems.entity(problemFor(status), Map.of());
    }

    static Problem problemFor(int status) {
        return switch (status) {
            case 400 -> Problems.badRequest(List.of()).problem();
            case 401 -> Problems.unauthorized();
            case 403 -> Problems.forbidden();
            case 404 -> Problems.unknownPath();
            case 405 -> Problems.methodNotAllowed("").problem();
            case 406 -> Problems.notAcceptable().problem();
            case 413 -> Problems.bodyTooLarge().problem();
            case 415 -> Problems.unsupportedMediaType().problem();
            case 503 -> Problems.serviceUnavailableProblem(Problems.STORE_DOWN_DETAIL);
            default ->
                status >= 400 && status < 500 ? Problems.badRequest(List.of()).problem() : Problems.internalError();
        };
    }
}

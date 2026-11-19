package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.problem.ApiException;
import eu.simpl.vocabulary.api.problem.Problem;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import eu.simpl.vocabulary.api.problem.Problems;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Errors raised by the servlet container outside Spring MVC also get a problem body (BLG-err-sanit). */
@RestController
public class ProblemErrorController implements ErrorController {

    private final ProblemWriter problemWriter;

    public ProblemErrorController(ProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @RequestMapping("/error")
    public ResponseEntity<Problem> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 500;
        ApiException problem = switch (status) {
            case 400 -> Problems.rejectedRequest();
            case 401 -> Problems.unauthorized();
            case 403 -> Problems.forbidden();
            case 404 -> Problems.pathNotFound();
            case 405 -> Problems.methodNotAllowed(null);
            case 406 -> Problems.notAcceptable();
            case 413 -> Problems.payloadTooLarge();
            case 415 -> Problems.unsupportedMediaType();
            case 503 -> Problems.storeUnavailable();
            default -> status >= 400 && status < 500 ? Problems.rejectedRequest() : Problems.internalError();
        };
        return problemWriter.entity(problem);
    }
}

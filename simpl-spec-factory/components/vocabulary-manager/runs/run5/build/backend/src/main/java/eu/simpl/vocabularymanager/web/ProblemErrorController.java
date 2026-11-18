package eu.simpl.vocabularymanager.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Errors raised outside Spring MVC (for example by the servlet container) also get a problem body. */
@RestController
public class ProblemErrorController implements ErrorController {

    private final ProblemWriter writer;

    public ProblemErrorController(ProblemWriter writer) {
        this.writer = writer;
    }

    @RequestMapping("/error")
    public ResponseEntity<Problem> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer i ? i : 500;
        ApiException e = switch (status) {
            case 400 ->
                ApiException.badRequest(Issue.schemaViolation("body", "body", "The request could not be read.", null));
            case 404 -> ApiException.noSuchResource();
            case 405 -> ApiException.methodNotAllowed(null);
            case 413 -> ApiException.payloadTooLarge("vocabularyFile");
            default -> ApiException.internal();
        };
        return writer.entity(e);
    }
}

package eu.simpl.vocabularymanager.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The container error page (ERROR dispatch only): errors raised outside Spring MVC still get a problem+json body.
 * A normal request to this path never reaches it: it matches no operation, so it gets 401 or 404 first (D84).
 */
@RestController
public class ProblemErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ProblemBody> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status instanceof Integer value ? value : 500;
        ProblemException problem = switch (code) {
            case 400 -> Problems.firewallRejected();
            case 404 -> Problems.pathNotFound();
            case 405 -> Problems.methodNotAllowed(null);
            case 413 -> Problems.uploadTooLarge();
            default -> Problems.internalError();
        };
        return ProblemHandler.toResponse(problem);
    }
}

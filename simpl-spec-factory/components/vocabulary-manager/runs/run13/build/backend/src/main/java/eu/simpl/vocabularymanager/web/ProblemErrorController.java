package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Error page of the servlet container (D158): answers that no handler produced (for example a response
 * error raised by the container) are problem details too. A direct request to this path gets 404.
 */
@RestController
public class ProblemErrorController implements ErrorController {

    public static final String PATH = "/error";

    @RequestMapping(PATH)
    public ResponseEntity<byte[]> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (!(status instanceof Integer code) || code < 400) {
            return ApiExceptionHandler.problem(ApiExceptionHandler.notFound());
        }
        ProblemType type = ProblemType.forStatus(code);
        String detail = code >= 500 ? "Internal Server Error" : type.title();
        return ApiExceptionHandler.problem(new ApiException(type, detail));
    }
}

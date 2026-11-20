package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.ProblemWriter;
import eu.simpl.vocabularymanager.api.Problems;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Errors sent outside the API code (sendError) as problem+json, never an HTML page or a stack trace. */
@RestController
public class ProblemErrorController implements ErrorController {

    /**
     * @param request the error dispatch
     * @return the problem response
     */
    @RequestMapping("/error")
    public ResponseEntity<byte[]> error(HttpServletRequest request) {
        if (request.getDispatcherType() != DispatcherType.ERROR) {
            return ProblemWriter.entity(Problems.unknownResource());
        }
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 500;
        return ProblemWriter.entity(Problems.generic(status, "HTTP " + status));
    }
}

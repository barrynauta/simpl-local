package eu.simpl.catalogue.cat.api;

import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.ProblemTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/** Out-of-scope operations answer 501 (D10, as revised in run 1.2: only the session and PUT /schemas/{schemaId}). */
@RestController
public class OutOfScopeController {
    private static ApiException notImplemented() {
        return new ApiException(ProblemTypes.NOT_IMPLEMENTED, "This operation is not implemented in this build.");
    }

    @GetMapping("/session") public void session() { throw notImplemented(); }
    @DeleteMapping("/session") public void logout() { throw notImplemented(); }
    @PutMapping("/schemas/{schemaId}") public void put() { throw notImplemented(); }
}

package eu.simpl.catalogue.cat.api;

import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.ProblemTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/** Out-of-scope operations answer 501 (D10). */
@RestController
public class OutOfScopeController {
    private static ApiException notImplemented() {
        return new ApiException(ProblemTypes.NOT_IMPLEMENTED, "This operation is not implemented in this build.");
    }

    @GetMapping("/query") public void queryPage() { throw notImplemented(); }
    @PostMapping("/query") public void query() { throw notImplemented(); }
    @GetMapping("/session") public void session() { throw notImplemented(); }
    @DeleteMapping("/session") public void logout() { throw notImplemented(); }
    @GetMapping("/verification") public void verifyPage() { throw notImplemented(); }
    @GetMapping("/schemas/latest") public void latest() { throw notImplemented(); }
    @PutMapping("/schemas/{schemaId}") public void put() { throw notImplemented(); }
}

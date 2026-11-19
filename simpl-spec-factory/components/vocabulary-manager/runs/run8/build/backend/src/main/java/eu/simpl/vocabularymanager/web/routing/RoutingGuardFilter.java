package eu.simpl.vocabularymanager.web.routing;

import eu.simpl.vocabularymanager.web.problem.ProblemWriter;
import eu.simpl.vocabularymanager.web.problem.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers 405 before anything else runs (D67, D77): for a method the server does not know, and for a method the
 * addressed path does not declare. Runs before the firewall and the token check.
 */
public class RoutingGuardFilter extends OncePerRequestFilter {

    private static final Set<String> KNOWN_METHODS =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT");

    private final OperationCatalogue catalogue;
    private final ProblemWriter problemWriter;

    /**
     * @param catalogue the operations of the contract
     * @param problemWriter writes the 405 body
     */
    public RoutingGuardFilter(OperationCatalogue catalogue, ProblemWriter problemWriter) {
        this.catalogue = catalogue;
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        Optional<String> path = catalogue.matchPath(request.getRequestURI());
        if (!KNOWN_METHODS.contains(method)) {
            Map<String, String> headers = path.isPresent()
                    ? Map.of("Allow", String.join(", ", catalogue.methodsOf(path.get())))
                    : Map.of();
            problemWriter.write(response, Problems.methodNotAllowed(method), headers);
            return;
        }
        if (path.isPresent()) {
            Set<String> declared = catalogue.methodsOf(path.get());
            if (!declared.contains(method)) {
                problemWriter.write(
                        response, Problems.methodNotAllowed(method), Map.of("Allow", String.join(", ", declared)));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

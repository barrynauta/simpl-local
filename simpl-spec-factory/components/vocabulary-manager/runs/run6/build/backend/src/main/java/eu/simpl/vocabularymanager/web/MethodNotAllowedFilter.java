package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.problem.ProblemException;
import eu.simpl.vocabularymanager.problem.ProblemWriter;
import eu.simpl.vocabularymanager.problem.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers 405 for every method a path does not declare in the OpenAPI file, including HEAD and OPTIONS, and 404
 * for paths outside the API. Runs after the security filters (401 and 403 come first, decision D17).
 */
@Component
@Order(0)
public class MethodNotAllowedFilter extends OncePerRequestFilter {

    private final ProblemWriter problems;

    /**
     * Creates the filter.
     *
     * @param problems the problem writer
     */
    public MethodNotAllowedFilter(ProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Optional<RouteTable.Route> route = RouteTable.find(path);
        if (route.isEmpty()) {
            if (path.startsWith("/actuator")) {
                chain.doFilter(request, response);
                return;
            }
            problems.write(response, Problems.unknownPath(), java.util.Map.of());
            return;
        }
        if (!route.get().methods().contains(request.getMethod())) {
            ProblemException e = Problems.methodNotAllowed(route.get().allow());
            problems.write(response, e.problem(), e.headers());
            return;
        }
        chain.doFilter(request, response);
    }
}

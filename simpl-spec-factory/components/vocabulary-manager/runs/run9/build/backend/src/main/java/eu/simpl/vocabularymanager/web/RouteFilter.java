package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.ProblemWriter;
import eu.simpl.vocabularymanager.api.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before the security filters (D67): an unknown method, or a method a known path does not declare, gives 405
 * (D77, finish line 5). Puts the operationId in the log context (D96);
 * {@link CorrelationFilter} clears it.
 */
public class RouteFilter extends OncePerRequestFilter {

    private final RouteTable routes;

    /** @param routes the route table */
    public RouteFilter(RouteTable routes) {
        this.routes = routes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        List<Route> forPath = routes.forPath(path);
        if (!forPath.isEmpty()) {
            Optional<Route> route =
                    forPath.stream().filter(r -> r.method().equals(method)).findFirst();
            if (route.isEmpty()) {
                ProblemWriter.write(response, Problems.methodNotAllowed(RouteTable.allow(forPath)));
                return;
            }
            MDC.put("operationId", route.get().operationId());
        } else if (!RouteTable.KNOWN_METHODS.contains(method)) {
            ProblemWriter.write(response, Problems.methodNotAllowed(null));
            return;
        }
        chain.doFilter(request, response);
    }
}

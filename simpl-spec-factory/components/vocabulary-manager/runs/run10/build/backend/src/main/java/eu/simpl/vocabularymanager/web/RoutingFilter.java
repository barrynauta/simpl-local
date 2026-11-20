package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter of the security chain (D67). It matches the request against the operation table: a method the server
 * does not know, or a method the matched path does not offer, gets 405 before any token check. A matched operation
 * is stored as a request attribute for authorisation and the later checks.
 */
public class RoutingFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = RoutingFilter.class.getName() + ".operation";
    public static final String MDC_OPERATION = "operationId";
    private static final Set<String> KNOWN_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        Optional<ApiOperation.Route> route = ApiOperation.route(method, request.getRequestURI());
        if (!KNOWN_METHODS.contains(method)) {
            ProblemWriter.write(
                    response,
                    Problems.methodNotAllowed(
                            route.map(ApiOperation.Route::allowedMethods).orElse(null)));
            return;
        }
        if (route.isPresent()) {
            Optional<ApiOperation> operation = route.get().operation();
            if (operation.isEmpty()) {
                ProblemWriter.write(
                        response, Problems.methodNotAllowed(route.get().allowedMethods()));
                return;
            }
            request.setAttribute(ATTRIBUTE, operation.get());
            MDC.put(MDC_OPERATION, operation.get().operationId());
        }
        chain.doFilter(request, response);
    }

    /** The operation matched for this request, or null. */
    public static ApiOperation operationOf(HttpServletRequest request) {
        return (ApiOperation) request.getAttribute(ATTRIBUTE);
    }
}

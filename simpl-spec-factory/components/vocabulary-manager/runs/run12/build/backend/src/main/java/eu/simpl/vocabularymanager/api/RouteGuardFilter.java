package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before the token check: a method the path does not declare, a method the server does not
 * know, HEAD, OPTIONS and TRACE get 405 with Allow (D67, D126, D139, D166). Puts the operation and
 * the vocabulary name and version of a declared operation in the MDC (D96).
 */
public class RouteGuardFilter extends OncePerRequestFilter {

    public static final String MATCH_ATTRIBUTE = RouteGuardFilter.class.getName() + ".match";
    private static final Set<String> SERVED = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final ProblemWriter problems;

    public RouteGuardFilter(ProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        Optional<Routes.Match> match;
        try {
            match = Routes.match(method, path == null ? "/" : path);
        } catch (RuntimeException e) {
            match = Optional.empty();
        }
        if (match.isPresent()) {
            Routes.Match m = match.get();
            if (m.route() == null) {
                reject(response, m.allowed());
                return;
            }
            request.setAttribute(MATCH_ATTRIBUTE, m);
            MDC.put("operationId", m.route().operationId());
            putVariable(m.variables(), "vocabularyName", "vocabularyName");
            putVariable(m.variables(), "name", "vocabularyName");
            putVariable(m.variables(), "version", "version");
        } else if (!SERVED.contains(method)) {
            reject(response, Set.of());
            return;
        }
        chain.doFilter(request, response);
    }

    private static void putVariable(Map<String, String> vars, String key, String mdcKey) {
        String v = vars.get(key);
        if (v != null && v.length() <= 64 && v.matches("[A-Za-z0-9]+")) {
            MDC.put(mdcKey, v);
        }
    }

    private void reject(HttpServletResponse response, Set<String> allowed) throws IOException {
        problems.write(
                response,
                new ApiException(
                        ProblemType.METHOD_NOT_ALLOWED,
                        "The method is not allowed for this resource.",
                        List.of(),
                        Map.of("Allow", String.join(", ", allowed)),
                        false));
    }
}

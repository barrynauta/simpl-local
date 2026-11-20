package eu.simpl.vocabularymanager.web;

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
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects methods before anything else (D67, D126, D139): a method the server does not know gets
 * 405 on any path; a known method that a declared path does not declare (HEAD and OPTIONS
 * included) gets 405 with Allow, with or without a token.
 */
public class MethodGuardFilter extends OncePerRequestFilter {

    private static final Set<String> KNOWN =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE");

    private final ProblemWriter problems;

    public MethodGuardFilter(ProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        List<ApiRoutes.Route> routes = ApiRoutes.relative(request.getRequestURI())
                .map(ApiRoutes::matching)
                .orElse(List.of());
        boolean declaredPath = !routes.isEmpty();
        boolean declaredMethod = routes.stream().anyMatch(r -> r.method().equals(method));
        if (!KNOWN.contains(method) || (declaredPath && !declaredMethod)) {
            Map<String, String> headers = declaredPath ? Map.of("Allow", ApiRoutes.allow(routes)) : Map.of();
            problems.write(
                    response,
                    new ApiException(
                            ProblemType.METHOD_NOT_ALLOWED,
                            "Method " + safe(method) + " is not allowed on this resource.",
                            List.of(),
                            headers,
                            false));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String safe(String method) {
        return method.length() > 20 || !method.matches("[A-Z]+") ? "used" : method;
    }
}

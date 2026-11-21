package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.security.TokenVerifier;
import eu.simpl.vocabularymanager.web.OperationCatalog.Access;
import eu.simpl.vocabularymanager.web.OperationCatalog.Operation;
import eu.simpl.vocabularymanager.web.OperationCatalog.Route;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * The request gate. It runs before routing and applies the decided order of checks (D17, D51, D63, D67,
 * D84, D139, D162): unknown method 405, firewall 400, declared path with another method 405, token 401 or
 * 503, role 403, Accept 406. Requests to unknown paths are passed on only with a valid token, so they end
 * in 404; without a token they get 401.
 */
public class RequestGateFilter extends OncePerRequestFilter {

    public static final String OPERATION_ATTRIBUTE = "vm.operationId";
    public static final String OPERATION_MDC = "operationId";
    private static final Logger LOG = LoggerFactory.getLogger(RequestGateFilter.class);
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("security");
    private static final Pattern CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private static final String BEARER = "bearer ";

    private final TokenVerifier tokenVerifier;
    private final HttpFirewall firewall;
    private final UrlPathHelper pathHelper = new UrlPathHelper();

    public RequestGateFilter(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
        StrictHttpFirewall strict = new StrictHttpFirewall();
        strict.setUnsafeAllowAnyHttpMethod(true);
        this.firewall = strict;
        pathHelper.setRemoveSemicolonContent(false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationId(request);
        MDC.put(TraceHeaders.CORRELATION_MDC, correlationId);
        response.setHeader(TraceHeaders.CORRELATION_HEADER, correlationId);
        addSecurityHeaders(response);
        try {
            HttpServletRequest checked = check(request, response);
            if (checked != null) {
                chain.doFilter(checked, response);
            }
        } catch (ApiException e) {
            ProblemWriter.write(response, e);
        } finally {
            MDC.clear();
        }
    }

    /** Runs the checks; returns the request to pass on, or null when an answer was written. */
    private HttpServletRequest check(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        if (!OperationCatalog.KNOWN_METHODS.contains(method)) {
            throw methodNotAllowed(List.of(), method);
        }
        try {
            firewall.getFirewalledRequest(request);
        } catch (RequestRejectedException e) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The request was rejected.");
        }
        String path = pathHelper.getPathWithinApplication(request);
        Route route = OperationCatalog.route(method, path);
        if (route.pathKnown() && route.operation().isEmpty()) {
            throw methodNotAllowed(List.copyOf(route.allowedMethods()), method);
        }
        Operation operation = route.operation().orElse(null);
        if (operation != null) {
            request.setAttribute(OPERATION_ATTRIBUTE, operation.operationId());
            MDC.put(OPERATION_MDC, operation.operationId());
        }
        if (operation != null && operation.access() == Access.PUBLIC) {
            checkAccept(request, operation);
            return withoutAccept(request);
        }
        AuthenticatedUser user = authenticate(request);
        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
        MDC.put("userId", user.subject());
        if (operation == null) {
            return request;
        }
        boolean allowed = operation.access() == Access.READ ? user.canRead() : user.canWrite();
        if (!allowed) {
            SECURITY_LOG.warn("Access denied to {}", operation.operationId());
            throw new ApiException(ProblemType.FORBIDDEN, "You are not authorized to perform this action.");
        }
        checkAccept(request, operation);
        return withoutAccept(request);
    }

    private AuthenticatedUser authenticate(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders("Authorization");
        List<String> values = headers == null ? List.of() : Collections.list(headers);
        if (values.size() != 1) {
            throw unauthorized(values.isEmpty() ? null : "invalid_request");
        }
        String value = values.get(0);
        if (value.length() <= BEARER.length() || !value.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw unauthorized("invalid_request");
        }
        try {
            return tokenVerifier.verify(value.substring(BEARER.length()).trim());
        } catch (TokenVerifier.InvalidTokenException e) {
            SECURITY_LOG.warn("Token rejected: {}", e.getMessage());
            throw unauthorized("invalid_token");
        }
    }

    private static ApiException unauthorized(String error) {
        String challenge = error == null ? "Bearer realm=\"simpl\"" : "Bearer realm=\"simpl\", error=\"" + error + "\"";
        return new ApiException(
                ProblemType.UNAUTHORIZED,
                "A valid bearer token is required.",
                List.of(),
                Map.of("WWW-Authenticate", challenge),
                false);
    }

    private static ApiException methodNotAllowed(List<String> allowed, String method) {
        LOG.debug("Method {} not allowed", method.length() > 16 ? method.substring(0, 16) : method);
        return new ApiException(
                ProblemType.METHOD_NOT_ALLOWED,
                "The method is not allowed on this resource.",
                List.of(),
                Map.of("Allow", String.join(", ", allowed)),
                false);
    }

    private static void checkAccept(HttpServletRequest request, Operation operation) {
        if (!AcceptCheck.acceptable(Collections.list(request.getHeaders("Accept")), operation.produces())) {
            throw new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served.");
        }
    }

    /** After the Accept check the handlers choose the media type themselves (D127, D151). */
    private static HttpServletRequest withoutAccept(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return "accept".equalsIgnoreCase(name) ? null : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return "accept".equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
            }
        };
    }

    private static String correlationId(HttpServletRequest request) {
        String given = request.getHeader(TraceHeaders.CORRELATION_HEADER);
        if (given != null && CORRELATION_ID.matcher(given).matches()) {
            return given;
        }
        return UUID.randomUUID().toString();
    }

    private static void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Vary", "Accept");
    }
}

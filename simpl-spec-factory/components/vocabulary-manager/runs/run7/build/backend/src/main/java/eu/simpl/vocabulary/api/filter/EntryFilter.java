package eu.simpl.vocabulary.api.filter;

import eu.simpl.vocabulary.api.RouteTable;
import eu.simpl.vocabulary.api.problem.ApiException;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.security.IdpUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter of every API request. It sets the correlation id (MSV-09) and applies the checks that come
 * before authentication (D67): unknown method 405, firewall 400, undeclared method on a known path 405
 * (D77). It also turns an unavailable identity provider into 503 (D44) and any unexpected failure in a
 * later filter into a problem response (A10, fail closed).
 */
public class EntryFilter extends OncePerRequestFilter {

    /** Request attribute holding the matched {@link RouteTable.Operation}. */
    public static final String OPERATION_ATTRIBUTE = EntryFilter.class.getName() + ".operation";

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_MDC = "correlationId";

    private static final Logger LOG = LoggerFactory.getLogger(EntryFilter.class);
    private static final Pattern SAFE_CORRELATION = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private final HttpFirewall firewall;
    private final ProblemWriter problemWriter;
    private final int managementPort;

    public EntryFilter(HttpFirewall firewall, ProblemWriter problemWriter, int managementPort) {
        this.firewall = firewall;
        this.problemWriter = problemWriter;
        this.managementPort = managementPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getLocalPort() == managementPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationId(request);
        MDC.put(CORRELATION_MDC, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        try {
            Optional<ApiException> early = earlyRejection(request);
            if (early.isPresent()) {
                problemWriter.write(response, early.get());
                return;
            }
            chain.doFilter(request, response);
        } catch (RuntimeException e) {
            handleUnexpected(response, e);
        } finally {
            MDC.remove(CORRELATION_MDC);
        }
    }

    private Optional<ApiException> earlyRejection(HttpServletRequest request) {
        String method = request.getMethod();
        Optional<RouteTable.PathMatch> match = RouteTable.match(request.getRequestURI());
        if (!RouteTable.KNOWN_METHODS.contains(method)) {
            return Optional.of(Problems.methodNotAllowed(
                    match.map(RouteTable.PathMatch::allowHeader).orElse(null)));
        }
        try {
            firewall.getFirewalledRequest(request);
        } catch (RequestRejectedException e) {
            LOG.info("Request rejected by the firewall: {}", e.getMessage());
            return Optional.of(Problems.rejectedRequest());
        }
        if (match.isPresent()) {
            Optional<RouteTable.Operation> operation = match.get().operation(method);
            if (operation.isEmpty()) {
                return Optional.of(Problems.methodNotAllowed(match.get().allowHeader()));
            }
            request.setAttribute(OPERATION_ATTRIBUTE, operation.get());
        }
        return Optional.empty();
    }

    private void handleUnexpected(HttpServletResponse response, RuntimeException e) throws IOException {
        if (causedBy(e, IdpUnavailableException.class)) {
            LOG.warn("Token could not be checked, identity provider unavailable: {}", e.getMessage());
            problemWriter.write(response, Problems.serviceUnavailable(Problems.IDP_UNAVAILABLE));
            return;
        }
        if (e instanceof ApiException apiException) {
            problemWriter.write(response, apiException);
            return;
        }
        LOG.error("Unexpected failure while processing the request", e);
        problemWriter.write(response, Problems.internalError());
    }

    /**
     * Tells whether the throwable or one of its causes has the given type.
     *
     * @param throwable the throwable
     * @param type the type
     * @return true when found
     */
    public static boolean causedBy(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 16) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }

    private static String correlationId(HttpServletRequest request) {
        String given = request.getHeader(CORRELATION_HEADER);
        if (given != null && SAFE_CORRELATION.matcher(given).matches()) {
            return given;
        }
        return UUID.randomUUID().toString();
    }
}

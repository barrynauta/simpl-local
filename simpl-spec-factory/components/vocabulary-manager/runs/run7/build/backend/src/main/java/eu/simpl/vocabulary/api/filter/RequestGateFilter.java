package eu.simpl.vocabulary.api.filter;

import eu.simpl.vocabulary.api.RouteTable;
import eu.simpl.vocabulary.api.RouteTable.Operation;
import eu.simpl.vocabulary.api.problem.ApiException;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.security.Roles;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks that run after authentication and before the operation, in the decided order (D51, D63):
 * 403 role, 406 Accept, 415 Content-Type, 413 declared size.
 */
public class RequestGateFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestGateFilter.class);
    private static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");

    private final ProblemWriter problemWriter;
    private final AppProperties.Limits limits;

    public RequestGateFilter(ProblemWriter problemWriter, AppProperties.Limits limits) {
        this.problemWriter = problemWriter;
        this.limits = limits;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Object attribute = request.getAttribute(EntryFilter.OPERATION_ATTRIBUTE);
        if (attribute instanceof Operation operation) {
            Optional<ApiException> rejection = check(request, operation);
            if (rejection.isPresent()) {
                problemWriter.write(response, rejection.get());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Applies the gate checks in order.
     *
     * @param request the request
     * @param operation the matched operation
     * @return the first failure, if any
     */
    Optional<ApiException> check(HttpServletRequest request, Operation operation) {
        if (!hasAccess(operation.access())) {
            LOG.warn("Access denied: {} {} for user {}", request.getMethod(), request.getRequestURI(), Roles.userId());
            return Optional.of(Problems.forbidden());
        }
        if (!acceptable(Collections.list(request.getHeaders("Accept")), operation.response())) {
            return Optional.of(Problems.notAcceptable());
        }
        if (!supportedContentType(request.getContentType(), operation.body())) {
            return Optional.of(Problems.unsupportedMediaType());
        }
        if (tooLarge(request.getContentLengthLong(), operation.body())) {
            return Optional.of(Problems.payloadTooLarge());
        }
        return Optional.empty();
    }

    private static boolean hasAccess(RouteTable.Access access) {
        if (access == RouteTable.Access.PUBLIC) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        boolean admin = Roles.has(authentication, Roles.ADMIN);
        if (access == RouteTable.Access.ADMIN) {
            return admin;
        }
        return admin || Roles.has(authentication, Roles.VIEWER);
    }

    /**
     * Applies the Accept rule of D63.
     *
     * @param acceptHeaders all Accept header values
     * @param kind response kind of the operation
     * @return true when the response can be served
     */
    static boolean acceptable(List<String> acceptHeaders, RouteTable.ResponseKind kind) {
        if (acceptHeaders.isEmpty()) {
            return true;
        }
        List<MediaType> ranges;
        try {
            ranges = MediaType.parseMediaTypes(acceptHeaders);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
        if (ranges.isEmpty()) {
            return true;
        }
        for (MediaType range : ranges) {
            if (range.getQualityValue() <= 0.0) {
                continue;
            }
            if (range.isWildcardType() && range.isWildcardSubtype()) {
                return true;
            }
            if (matchesKind(range, kind)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesKind(MediaType range, RouteTable.ResponseKind kind) {
        return switch (kind) {
            case TURTLE -> sameType(range, TURTLE);
            case JSON, NONE ->
                sameType(range, MediaType.APPLICATION_JSON) || sameType(range, MediaType.APPLICATION_PROBLEM_JSON);
        };
    }

    private static boolean sameType(MediaType a, MediaType b) {
        return a.getType().equalsIgnoreCase(b.getType()) && a.getSubtype().equalsIgnoreCase(b.getSubtype());
    }

    /**
     * Applies the Content-Type rule of D15.
     *
     * @param contentType the Content-Type header
     * @param kind body kind of the operation
     * @return true when supported
     */
    static boolean supportedContentType(String contentType, RouteTable.BodyKind kind) {
        if (kind == RouteTable.BodyKind.NONE) {
            return true;
        }
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        MediaType type;
        try {
            type = MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
        MediaType expected =
                kind == RouteTable.BodyKind.JSON ? MediaType.APPLICATION_JSON : MediaType.MULTIPART_FORM_DATA;
        return sameType(type, expected);
    }

    private boolean tooLarge(long contentLength, RouteTable.BodyKind kind) {
        return switch (kind) {
            case MULTIPART -> contentLength > limits.maxRequestBytes();
            case JSON -> contentLength > limits.maxJsonBytes();
            case NONE -> false;
        };
    }
}

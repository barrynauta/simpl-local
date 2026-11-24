package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.security.JwtAuthenticator;
import eu.simpl.vocabularymanager.service.UploadSlots;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs the checks that come before any handler, in the decided order (D17, D51, D63, D67, D84, D139, D162, D170,
 * D172): unknown method 405, firewall 400, undeclared method 405 with Allow, token 401 (or 404 for an unknown
 * path), role 403, Accept 406, Content-Type 415, declared size 413 and the upload slot 503. A request that matches
 * no declared operation never reaches a handler (default deny).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatekeeperFilter extends OncePerRequestFilter {

    public static final String OPERATION = "vm.operation";
    public static final String USER = "vm.user";
    public static final String PATH_VALUES = "vm.pathValues";

    public static final long MULTIPART_LIMIT = 6L * 1024 * 1024;
    public static final long JSON_LIMIT = 1024L * 1024;
    public static final String JSON_TOO_LARGE = "The request body is larger than 1 MB.";
    public static final String MULTIPART_TOO_LARGE = "The request is larger than the maximum allowed size of 6 MB.";

    private static final Logger LOG = LoggerFactory.getLogger(GatekeeperFilter.class);
    private static final Set<String> KNOWN_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "TRACE", "PATCH", "CONNECT");
    private static final Pattern CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private static final Pattern FIREWALL = Pattern.compile(
            "(?i)(%2f|%5c|%25|%2e|%00|%0[0-9a-f]|%1[0-9a-f]|%7f|;|\\\\|//|/\\./|/\\.\\./|/\\.$|/\\.\\.$|[\\x00-\\x1f\\x7f])");

    private final RouteMatcher routeMatcher;
    private final JwtAuthenticator authenticator;
    private final UploadSlots uploadSlots;
    private final MeterRegistry meterRegistry;

    public GatekeeperFilter(
            RouteMatcher routeMatcher,
            JwtAuthenticator authenticator,
            UploadSlots uploadSlots,
            MeterRegistry meterRegistry) {
        this.routeMatcher = routeMatcher;
        this.authenticator = authenticator;
        this.uploadSlots = uploadSlots;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationId(request);
        MDC.put(TraceHeaders.CORRELATION_MDC, correlationId);
        response.setHeader(TraceHeaders.CORRELATION_HEADER, correlationId);
        Timer.Sample sample = Timer.start(meterRegistry);
        String operationId = "unknown";
        boolean slotHeld = false;
        try {
            Operation operation = admit(request);
            operationId = operation.operationId();
            slotHeld = needsSlot(operation);
            chain.doFilter(request, response);
        } catch (ApiException e) {
            if (!response.isCommitted()) {
                ProblemWriter.write(response, e);
            }
        } finally {
            if (slotHeld) {
                uploadSlots.release();
            }
            sample.stop(meterRegistry.timer(
                    "vocabulary.api.requests",
                    "operation",
                    operationId,
                    "status",
                    String.valueOf(response.getStatus())));
            MDC.remove(TraceHeaders.CORRELATION_MDC);
            MDC.remove("operationId");
            MDC.remove("userId");
            MDC.remove("vocabularyName");
            MDC.remove("version");
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Runs all checks before the handler. Returns the operation when the request may proceed.
     *
     * @throws ApiException with the first failing check
     */
    private Operation admit(HttpServletRequest request) {
        String method = request.getMethod();
        String rawPath = request.getRequestURI();
        RouteMatcher.Match match = routeMatcher.match(method, rawPath);
        if (!KNOWN_METHODS.contains(method)) {
            throw methodNotAllowed(match);
        }
        if (FIREWALL.matcher(rawPath).find()) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The request path is not allowed.", List.of());
        }
        if (match.pathDeclared() && match.operation() == null) {
            throw methodNotAllowed(match);
        }
        if (!match.pathDeclared()) {
            if ("TRACE".equals(method)) {
                throw methodNotAllowed(match);
            }
            authenticate(request);
            throw new ApiException(ProblemType.NOT_FOUND, "The requested resource was not found.", List.of());
        }
        Operation operation = match.operation();
        MDC.put("operationId", operation.operationId());
        putPathMdc(match.pathValues());
        request.setAttribute(OPERATION, operation);
        request.setAttribute(PATH_VALUES, match.pathValues());
        if (operation.access() != Operation.Access.PUBLIC) {
            AuthenticatedUser user = authenticate(request);
            authorize(operation, user);
            request.setAttribute(USER, user);
        }
        checkAccept(operation, request);
        checkContentType(operation, request);
        checkSize(operation, request);
        if (needsSlot(operation) && !uploadSlots.tryAcquire()) {
            throw ApiException.unavailable("Too many uploads in progress. Retry later.", null);
        }
        return operation;
    }

    private AuthenticatedUser authenticate(HttpServletRequest request) {
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.size() > 1) {
            LOG.warn("Authentication failed: more than one Authorization header");
            throw new ApiException(ProblemType.UNAUTHORIZED, null, List.of());
        }
        AuthenticatedUser user = authenticator.authenticate(headers.isEmpty() ? null : headers.get(0));
        MDC.put("userId", user.subject());
        return user;
    }

    private static void authorize(Operation operation, AuthenticatedUser user) {
        boolean allowed = operation.access() == Operation.Access.ADMIN ? user.canWrite() : user.canRead();
        if (!allowed) {
            LOG.warn("Access denied: user {} lacks the role for {}", user.subject(), operation.operationId());
            throw new ApiException(ProblemType.FORBIDDEN, null, List.of());
        }
    }

    private static void checkAccept(Operation operation, HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders("Accept"));
        if (values.isEmpty()) {
            return;
        }
        Set<String> acceptable = operation.reply() == Operation.Reply.TURTLE
                ? Set.of("text/turtle", "*/*", "application/*")
                : Set.of("application/json", "application/problem+json", "*/*", "application/*");
        for (String value : values) {
            for (String range : value.split(",")) {
                if (rangeAccepted(range.trim(), acceptable)) {
                    return;
                }
            }
        }
        throw new ApiException(ProblemType.NOT_ACCEPTABLE, null, List.of());
    }

    private static boolean rangeAccepted(String range, Set<String> acceptable) {
        if (range.isEmpty()) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(range);
            if (mediaType.getQualityValue() <= 0.0) {
                return false;
            }
            String type = (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase(Locale.ROOT);
            return acceptable.contains(type);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void checkContentType(Operation operation, HttpServletRequest request) {
        if (operation.body() == Operation.Body.NONE) {
            return;
        }
        String expected = operation.body() == Operation.Body.JSON ? "application/json" : "multipart/form-data";
        String contentType = request.getContentType();
        if (contentType == null) {
            throw new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, null, List.of());
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            String type = (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase(Locale.ROOT);
            if (!expected.equals(type)) {
                throw new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, null, List.of());
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, null, List.of());
        }
    }

    private static void checkSize(Operation operation, HttpServletRequest request) {
        long length = request.getContentLengthLong();
        if (operation.body() == Operation.Body.JSON && length > JSON_LIMIT) {
            throw new ApiException(ProblemType.PAYLOAD_TOO_LARGE, JSON_TOO_LARGE, List.of());
        }
        if (operation.body() == Operation.Body.MULTIPART && length > MULTIPART_LIMIT) {
            throw new ApiException(ProblemType.PAYLOAD_TOO_LARGE, MULTIPART_TOO_LARGE, List.of());
        }
    }

    private static boolean needsSlot(Operation operation) {
        return operation == Operation.CREATE_VOCABULARY || operation == Operation.CREATE_VOCABULARY_VERSION;
    }

    private static ApiException methodNotAllowed(RouteMatcher.Match match) {
        Map<String, String> headers =
                match.pathDeclared() ? Map.of("Allow", String.join(", ", match.allowedMethods())) : Map.of();
        return new ApiException(ProblemType.METHOD_NOT_ALLOWED, null, List.<Issue>of(), headers, null);
    }

    private static void putPathMdc(Map<String, String> pathValues) {
        String name = pathValues.getOrDefault("vocabularyName", pathValues.get("name"));
        if (name != null) {
            String decoded = QueryParameters.decode(name);
            if (decoded != null && decoded.matches("^[A-Za-z0-9]{1,64}$")) {
                MDC.put("vocabularyName", decoded);
            }
        }
        String version = pathValues.get("version");
        if (version != null && version.matches("^[0-9]{1,10}$")) {
            MDC.put("version", version);
        }
    }

    private static String correlationId(HttpServletRequest request) {
        String value = request.getHeader(TraceHeaders.CORRELATION_HEADER);
        if (value != null && CORRELATION_ID.matcher(value).matches()) {
            return value;
        }
        return UUID.randomUUID().toString();
    }
}

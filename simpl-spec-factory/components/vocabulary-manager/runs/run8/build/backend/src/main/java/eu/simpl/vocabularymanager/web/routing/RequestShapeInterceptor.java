package eu.simpl.vocabularymanager.web.routing;

import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Request checks that run after authentication and authorisation, in the order of D51 and D63: 406 Accept, 415
 * Content-Type, 413 declared size, then 400 for path values (D49) and query parameters (D21, D52). Body checks follow
 * in the controllers.
 */
public class RequestShapeInterceptor implements HandlerInterceptor {

    /** Largest multipart request (D76). */
    public static final long MAX_MULTIPART_BYTES = 6L * 1024 * 1024;
    /** Largest JSON request body (NFR-04). */
    public static final long MAX_JSON_BYTES = 1024L * 1024;

    private static final Set<String> JSON_ACCEPT = Set.of("application/json", "application/problem+json", "*/*");
    private static final Set<String> TURTLE_ACCEPT = Set.of("text/turtle", "*/*");
    private static final Set<String> NAME_PARAMETERS = Set.of("vocabularyName", "name");

    private final OperationCatalogue catalogue;

    /**
     * @param catalogue the operations of the contract
     */
    public RequestShapeInterceptor(OperationCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ApiOperation operation = catalogue
                .resolve(request.getRequestURI(), request.getMethod())
                .orElse(null);
        if (operation == null) {
            return true;
        }
        checkAccept(request, operation);
        checkContentType(request, operation);
        checkDeclaredSize(request, operation);
        List<ProblemIssue> issues = new ArrayList<>(pathIssues(request));
        issues.addAll(queryIssues(request, operation));
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return true;
    }

    private static void checkAccept(HttpServletRequest request, ApiOperation operation) {
        List<String> headers = Collections.list(request.getHeaders("Accept"));
        if (headers.isEmpty()) {
            return;
        }
        Set<String> allowed = operation.response() == ApiOperation.ResponseKind.TURTLE ? TURTLE_ACCEPT : JSON_ACCEPT;
        try {
            for (MediaType mediaType : MediaType.parseMediaTypes(headers)) {
                String plain = mediaType.getType() + "/" + mediaType.getSubtype();
                if (mediaType.getQualityValue() > 0 && allowed.contains(plain.toLowerCase(java.util.Locale.ROOT))) {
                    return;
                }
            }
        } catch (IllegalArgumentException e) {
            throw Problems.notAcceptable();
        }
        throw Problems.notAcceptable();
    }

    private static void checkContentType(HttpServletRequest request, ApiOperation operation) {
        if (operation.requestBody() == ApiOperation.RequestBody.NONE) {
            return;
        }
        String header = request.getContentType();
        if (header == null) {
            throw Problems.unsupportedMediaType();
        }
        MediaType expected = operation.requestBody() == ApiOperation.RequestBody.JSON
                ? MediaType.APPLICATION_JSON
                : MediaType.MULTIPART_FORM_DATA;
        try {
            MediaType actual = MediaType.parseMediaType(header);
            if (!actual.getType().equalsIgnoreCase(expected.getType())
                    || !actual.getSubtype().equalsIgnoreCase(expected.getSubtype())) {
                throw Problems.unsupportedMediaType();
            }
        } catch (InvalidMediaTypeException e) {
            throw Problems.unsupportedMediaType();
        }
    }

    private static void checkDeclaredSize(HttpServletRequest request, ApiOperation operation) {
        long length = request.getContentLengthLong();
        if (operation.requestBody() == ApiOperation.RequestBody.MULTIPART && length > MAX_MULTIPART_BYTES) {
            throw Problems.payloadTooLarge("vocabularyFile");
        }
        if (operation.requestBody() == ApiOperation.RequestBody.JSON && length > MAX_JSON_BYTES) {
            throw Problems.payloadTooLarge("body");
        }
    }

    private static List<ProblemIssue> pathIssues(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        List<ProblemIssue> issues = new ArrayList<>();
        if (!(attribute instanceof Map<?, ?> variables)) {
            return issues;
        }
        for (Map.Entry<?, ?> entry : variables.entrySet()) {
            String name = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());
            if (NAME_PARAMETERS.contains(name) && !FieldRules.isValidName(value)) {
                issues.add(ProblemIssue.schemaViolation(
                        "path",
                        name,
                        "Path parameter '" + name + "' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
            } else if ("version".equals(name) && !FieldRules.isValidVersion(value)) {
                issues.add(ProblemIssue.schemaViolation(
                        "path", name, "Path parameter 'version' must be a positive integer."));
            }
        }
        return issues;
    }

    private static List<ProblemIssue> queryIssues(HttpServletRequest request, ApiOperation operation) {
        List<ProblemIssue> issues = new ArrayList<>();
        QueryParameters parameters;
        try {
            parameters = QueryParameters.parse(request.getQueryString());
        } catch (IllegalArgumentException e) {
            issues.add(ProblemIssue.schemaViolation("query", "", "The query string is not correctly encoded."));
            return issues;
        }
        for (String name : parameters.names()) {
            if (!operation.declaresQueryParameter(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "query", name, "Query parameter '" + shorten(name) + "' is not supported."));
            } else if (parameters.values(name).size() > 1) {
                issues.add(ProblemIssue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' may be given only once."));
            }
        }
        return issues;
    }

    private static String shorten(String value) {
        return value.length() > 64 ? value.substring(0, 64) + "..." : value;
    }
}

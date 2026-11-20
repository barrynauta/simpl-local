package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after authentication and authorisation (D51, D63, D67, D84): a request that matches no declared operation gets
 * 404 and never reaches a handler; then the Accept header (406) and the request media type (415) are checked.
 */
public class MediaTypeFilter extends OncePerRequestFilter {

    private static final Set<String> JSON_ACCEPT = Set.of("application/json", "application/problem+json", "*/*");
    private static final Set<String> TURTLE_ACCEPT = Set.of("text/turtle", "*/*");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ApiOperation operation = RoutingFilter.operationOf(request);
        try {
            if (operation == null) {
                throw Problems.pathNotFound();
            }
            checkAccept(operation, request.getHeaders("Accept"));
            checkContentType(operation, request.getContentType());
        } catch (ProblemException problem) {
            ProblemWriter.write(response, problem);
            return;
        }
        chain.doFilter(new AcceptStrippedRequest(request), response);
    }

    static void checkAccept(ApiOperation operation, Enumeration<String> headers) {
        List<String> values = headers == null ? List.of() : Collections.list(headers);
        if (values.isEmpty()) {
            return;
        }
        Set<String> acceptable = operation.produces() == ApiOperation.Produces.TURTLE ? TURTLE_ACCEPT : JSON_ACCEPT;
        List<MediaType> ranges;
        try {
            ranges = MediaType.parseMediaTypes(values);
        } catch (InvalidMediaTypeException e) {
            throw Problems.notAcceptable();
        }
        if (ranges.isEmpty()) {
            return;
        }
        boolean ok = ranges.stream()
                .filter(range -> range.getQualityValue() > 0)
                .anyMatch(range -> acceptable.contains(range.getType() + "/" + range.getSubtype()));
        if (!ok) {
            throw Problems.notAcceptable();
        }
    }

    static void checkContentType(ApiOperation operation, String contentType) {
        switch (operation.bodyKind()) {
            case JSON -> requireType(contentType, "application", "json");
            case MULTIPART -> requireType(contentType, "multipart", "form-data");
            case NONE -> {
                // No body is declared; a body that is sent is ignored.
            }
        }
    }

    private static void requireType(String contentType, String type, String subtype) {
        if (contentType == null) {
            throw Problems.unsupportedMediaType();
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if (!mediaType.getType().equals(type) || !mediaType.getSubtype().equals(subtype)) {
                throw Problems.unsupportedMediaType();
            }
        } catch (InvalidMediaTypeException e) {
            throw Problems.unsupportedMediaType();
        }
    }

    /** Hides the Accept header from Spring MVC once it has been checked, so the handler decides the media type. */
    static final class AcceptStrippedRequest extends HttpServletRequestWrapper {

        AcceptStrippedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return "accept".equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return "accept".equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }
    }
}

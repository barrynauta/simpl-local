package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Media type and size checks after authentication (D51, D63, D127): 406 for an Accept header the
 * operation cannot serve, then 415 for a wrong Content-Type, then 413 for a declared body size
 * above the limit (D76, D110). The Accept header is then set to the operation's media type, so the
 * response always has its normal media type.
 */
public class NegotiationFilter extends OncePerRequestFilter {

    private static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");
    private static final MediaType PROBLEM = MediaType.parseMediaType(ProblemWriter.PROBLEM_JSON);
    private static final MediaType APPLICATION_ANY = MediaType.parseMediaType("application/*");

    private final ProblemWriter problems;
    private final AppProperties.Limits limits;

    public NegotiationFilter(ProblemWriter problems, AppProperties properties) {
        this.problems = problems;
        this.limits = properties.limits();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<ApiRoutes.Route> found =
                ApiRoutes.relative(request.getRequestURI()).flatMap(path -> ApiRoutes.find(request.getMethod(), path));
        if (found.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        ApiRoutes.Route route = found.get();
        try {
            checkAccept(request, route);
            checkContentType(request, route);
            checkLength(request, route);
        } catch (ApiException ex) {
            problems.write(response, ex);
            return;
        }
        chain.doFilter(
                new FixedAccept(
                        request, route.produces() == ApiRoutes.Produces.TURTLE ? "text/turtle" : "application/json"),
                response);
    }

    static void checkAccept(HttpServletRequest request, ApiRoutes.Route route) {
        Enumeration<String> headers = request.getHeaders("Accept");
        if (headers == null || !headers.hasMoreElements()) {
            return;
        }
        String joined = String.join(",", Collections.list(headers));
        if (joined.isBlank()) {
            return;
        }
        List<MediaType> accepted;
        try {
            accepted = MediaType.parseMediaTypes(joined);
        } catch (IllegalArgumentException ex) {
            throw notAcceptable();
        }
        List<MediaType> served = route.produces() == ApiRoutes.Produces.TURTLE
                ? List.of(TURTLE)
                : List.of(MediaType.APPLICATION_JSON, PROBLEM);
        for (MediaType type : accepted) {
            if (type.getQualityValue() <= 0) {
                continue;
            }
            if (type.isWildcardType() || type.equalsTypeAndSubtype(APPLICATION_ANY)) {
                return;
            }
            for (MediaType candidate : served) {
                if (type.equalsTypeAndSubtype(candidate)) {
                    return;
                }
            }
        }
        throw notAcceptable();
    }

    static void checkContentType(HttpServletRequest request, ApiRoutes.Route route) {
        if (route.consumes() == ApiRoutes.Consumes.NONE) {
            return;
        }
        MediaType type;
        try {
            String header = request.getContentType();
            if (header == null || header.isBlank()) {
                throw unsupported();
            }
            type = MediaType.parseMediaType(header);
        } catch (InvalidMediaTypeException ex) {
            throw unsupported();
        }
        boolean ok = route.consumes() == ApiRoutes.Consumes.JSON
                ? type.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)
                : type.equalsTypeAndSubtype(MediaType.MULTIPART_FORM_DATA) && type.getParameter("boundary") != null;
        if (!ok) {
            throw unsupported();
        }
    }

    void checkLength(HttpServletRequest request, ApiRoutes.Route route) {
        long length = request.getContentLengthLong();
        if (route.consumes() == ApiRoutes.Consumes.JSON && length > limits.maxJsonBytes()) {
            throw ApiException.tooLarge("body", "The request body is larger than 1 MB.");
        }
        if (route.consumes() == ApiRoutes.Consumes.MULTIPART && length > limits.maxMultipartBytes()) {
            throw ApiException.tooLarge(MultipartForm.ContentField.FILE, MultipartForm.TOO_LARGE);
        }
    }

    private static ApiException notAcceptable() {
        return new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served.");
    }

    private static ApiException unsupported() {
        return new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported.");
    }

    /** Presents the operation's normal media type as the Accept header. */
    static final class FixedAccept extends HttpServletRequestWrapper {
        private final String accept;

        FixedAccept(HttpServletRequest request, String accept) {
            super(request);
            this.accept = accept;
        }

        @Override
        public String getHeader(String name) {
            return "Accept".equalsIgnoreCase(name) ? accept : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return "Accept".equalsIgnoreCase(name) ? Collections.enumeration(List.of(accept)) : super.getHeaders(name);
        }
    }
}

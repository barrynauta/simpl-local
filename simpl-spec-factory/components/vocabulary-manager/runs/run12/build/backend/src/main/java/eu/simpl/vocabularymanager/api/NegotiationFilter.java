package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after the token and role checks and before the operation: 406 (D63, D127, D151), then 415
 * (D15), then 413 for the request size (D76, D110, D150), in the order of D51. The Accept header is
 * then set to the operation's own media type so the framework mapping always matches.
 */
public class NegotiationFilter extends OncePerRequestFilter {

    public static final long JSON_LIMIT = 1024L * 1024L;
    public static final long MULTIPART_LIMIT = 6L * 1024L * 1024L;

    private static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");
    private static final MediaType PROBLEM = MediaType.parseMediaType("application/problem+json");

    private final ProblemWriter problems;

    public NegotiationFilter(ProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Object attr = request.getAttribute(RouteGuardFilter.MATCH_ATTRIBUTE);
        if (!(attr instanceof Routes.Match match) || match.route() == null) {
            chain.doFilter(request, response);
            return;
        }
        Routes.Route route = match.route();
        response.setHeader("Vary", "Accept");
        try {
            MediaType produced = route.turtle() ? TURTLE : MediaType.APPLICATION_JSON;
            if (!acceptable(request.getHeader("Accept"), route.turtle())) {
                throw new ApiException(ProblemType.NOT_ACCEPTABLE, "Requested media type cannot be served.");
            }
            if (route.body() != Routes.Body.NONE) {
                checkContentType(request.getContentType(), route.body());
                long length = request.getContentLengthLong();
                long limit = route.body() == Routes.Body.JSON ? JSON_LIMIT : MULTIPART_LIMIT;
                if (length > limit) {
                    throw route.body() == Routes.Body.JSON ? ApiException.jsonTooLarge() : ApiException.fileTooLarge();
                }
            }
            HttpServletRequest wrapped = new NegotiatedRequest(request, produced.toString(), route);
            chain.doFilter(wrapped, response);
        } catch (ApiException e) {
            problems.write(response, e);
        }
    }

    /** Accept rules of D63 and D127. */
    static boolean acceptable(String accept, boolean turtle) {
        if (accept == null || accept.isBlank()) {
            return true;
        }
        List<MediaType> types;
        try {
            types = MediaType.parseMediaTypes(accept);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
        for (MediaType t : types) {
            if (t.getQualityValue() <= 0.0) {
                continue;
            }
            if (t.isWildcardType()) {
                return true;
            }
            if (t.getType().equals("application") && t.isWildcardSubtype()) {
                return true;
            }
            if (turtle) {
                if (t.getType().equals("text")
                        && (t.isWildcardSubtype() || t.getSubtype().equals("turtle"))) {
                    return true;
                }
            } else if (t.getType().equals("application")
                    && (t.getSubtype().equals("json") || t.getSubtype().equals(PROBLEM.getSubtype()))) {
                return true;
            }
        }
        return false;
    }

    private static void checkContentType(String contentType, Routes.Body body) {
        MediaType ct = null;
        if (contentType != null) {
            try {
                ct = MediaType.parseMediaType(contentType);
            } catch (InvalidMediaTypeException e) {
                ct = null;
            }
        }
        boolean ok = ct != null
                && (body == Routes.Body.JSON
                        ? ct.getType().equals("application") && ct.getSubtype().equals("json")
                        : ct.getType().equals("multipart") && ct.getSubtype().equals("form-data"));
        if (!ok) {
            throw new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported.");
        }
    }

    /** Request with the operation's media type as Accept and a size-limited JSON body. */
    static final class NegotiatedRequest extends HttpServletRequestWrapper {
        private final String accept;
        private final Routes.Route route;
        private ServletInputStream limited;

        NegotiatedRequest(HttpServletRequest request, String accept, Routes.Route route) {
            super(request);
            this.accept = accept;
            this.route = route;
        }

        @Override
        public String getHeader(String name) {
            return "accept".equalsIgnoreCase(name) ? accept : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return "accept".equalsIgnoreCase(name) ? Collections.enumeration(List.of(accept)) : super.getHeaders(name);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (route.body() != Routes.Body.JSON) {
                return super.getInputStream();
            }
            if (limited == null) {
                limited = new LimitedInputStream(super.getInputStream(), JSON_LIMIT);
            }
            return limited;
        }
    }

    /** Thrown when a body without Content-Length grows past the limit. */
    public static final class BodyTooLargeException extends IOException {
        private static final long serialVersionUID = 1L;

        public BodyTooLargeException() {
            super("request body too large");
        }
    }

    static final class LimitedInputStream extends ServletInputStream {
        private final ServletInputStream in;
        private final long limit;
        private long count;

        LimitedInputStream(ServletInputStream in, long limit) {
            this.in = in;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b >= 0 && ++count > limit) {
                throw new BodyTooLargeException();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
            if (n > 0) {
                count += n;
                if (count > limit) {
                    throw new BodyTooLargeException();
                }
            }
            return n;
        }

        @Override
        public boolean isFinished() {
            return in.isFinished();
        }

        @Override
        public boolean isReady() {
            return in.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            in.setReadListener(listener);
        }
    }
}

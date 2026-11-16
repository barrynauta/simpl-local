package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Records every inbound call except the control and health endpoints (C.6 "calls"). */
@Component
public class CallRecorderFilter extends OncePerRequestFilter {

    private static final Pattern NID_IN_PATH = Pattern.compile("/(?:isPresent|isValid|signed|vcExchange)/([^/]+)");

    private final StubState state;
    private final ObjectMapper mapper;

    public CallRecorderFilter(StubState state, ObjectMapper mapper) {
        this.state = state;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/_control") || path.equals("/health")) {
            chain.doFilter(request, response);
            return;
        }
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 5 * 1024 * 1024);
        try {
            chain.doFilter(wrapped, response);
        } finally {
            String nid = request.getHeader("X-Contract-Negotiation-Id");
            if (nid == null) {
                Matcher m = NID_IN_PATH.matcher(path);
                if (m.find()) {
                    nid = m.group(1);
                }
            }
            if (nid == null) {
                nid = request.getParameter("contractNegotiationId");
            }
            Object body = null;
            byte[] bytes = wrapped.getContentAsByteArray();
            if (bytes.length > 0) {
                String text = new String(bytes, StandardCharsets.UTF_8);
                try {
                    body = mapper.readTree(text);
                } catch (Exception e) {
                    body = text;
                }
            }
            state.recordCall(new StubState.Call(Instant.now().toString(), request.getMethod(), path,
                    response.getStatus(), body, nid));
        }
    }
}

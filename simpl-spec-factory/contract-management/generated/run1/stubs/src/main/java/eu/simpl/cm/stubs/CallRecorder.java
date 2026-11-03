package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
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

/** Records every inbound call (C.6 /_control/calls). */
@Component
public class CallRecorder extends OncePerRequestFilter {
    private final StubState state;

    public CallRecorder(StubState state) {
        this.state = state;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 1_000_000);
        try {
            chain.doFilter(wrapped, response);
        } finally {
            String path = request.getRequestURI();
            if (!path.equals("/_control/calls") && !path.equals("/health")) {
                String raw = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
                JsonNode body = null;
                if (!raw.isBlank()) {
                    try {
                        body = state.mapper.readTree(raw);
                    } catch (Exception e) {
                        body = state.mapper.getNodeFactory().textNode(raw);
                    }
                }
                String query = request.getQueryString();
                state.calls.add(new StubState.Call(Instant.now(), request.getMethod(),
                        query == null ? path : path + "?" + query, response.getStatus(), body,
                        request.getHeader("X-Contract-Negotiation-Id")));
            }
        }
    }
}

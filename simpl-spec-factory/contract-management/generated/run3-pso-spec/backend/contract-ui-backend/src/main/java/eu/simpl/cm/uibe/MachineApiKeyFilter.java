package eu.simpl.cm.uibe;

import eu.simpl.cm.common.ProblemTypes;
import eu.simpl.cm.common.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** D66: machine operations need x-api-key equal to the UIBE key, else 401. Not a bean on purpose. */
public class MachineApiKeyFilter extends OncePerRequestFilter {

    private final byte[] key;

    public MachineApiKeyFilter(String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader("x-api-key");
        if (given == null || !MessageDigest.isEqual(key, given.getBytes(StandardCharsets.UTF_8))) {
            ProblemWriter.write(request, response, 401, ProblemTypes.UNAUTHORIZED, "Unauthorized",
                    given == null ? "Missing API Key" : "Invalid API Key");
            return;
        }
        chain.doFilter(request, response);
    }
}

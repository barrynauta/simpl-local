package eu.simpl.vocabularymanager.web;

import java.util.Set;

/**
 * One operation of the decided contract with its role rule (D16, D84).
 *
 * @param method HTTP method
 * @param template path template under /v1, parameters written as {name}
 * @param operationId operationId of the OpenAPI
 * @param roles realm roles that may call it; empty means public (only GET /health)
 */
public record Route(String method, String template, String operationId, Set<String> roles) {

    /** Path segments of the template. */
    public String[] segments() {
        return template.substring(1).split("/", -1);
    }

    /** Number of fixed (non-parameter) segments; more fixed segments win (D77). */
    public int fixedSegments() {
        int fixed = 0;
        for (String segment : segments()) {
            if (!segment.startsWith("{")) {
                fixed++;
            }
        }
        return fixed;
    }

    /** Whether the path segments fit the template. */
    public boolean matches(String[] pathSegments) {
        String[] own = segments();
        if (own.length != pathSegments.length) {
            return false;
        }
        for (int i = 0; i < own.length; i++) {
            if (pathSegments[i].isEmpty()) {
                return false;
            }
            if (!own[i].startsWith("{") && !own[i].equals(pathSegments[i])) {
                return false;
            }
        }
        return true;
    }

    /** Spring path pattern of the template. */
    public String pattern() {
        return template;
    }
}

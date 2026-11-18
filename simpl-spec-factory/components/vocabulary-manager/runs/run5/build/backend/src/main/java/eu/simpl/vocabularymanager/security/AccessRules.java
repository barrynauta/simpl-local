package eu.simpl.vocabularymanager.security;

import java.util.EnumSet;
import java.util.Set;

/** Which roles an operation needs (D16). GET /v1/health and paths outside the API base need no token (D17). */
public final class AccessRules {

    /** Either role. */
    public static final Set<Role> READ = EnumSet.of(Role.GA_VOCABULARY_VIEWER, Role.GA_VOCABULARY_ADMIN);
    /** Admin only. */
    public static final Set<Role> WRITE = EnumSet.of(Role.GA_VOCABULARY_ADMIN);

    private static final String API_BASE = "/v1";
    private static final Set<String> READ_ONLY_POSTS = Set.of(
            API_BASE + "/vocabularies/searches",
            API_BASE + "/external-vocabulary/searches",
            API_BASE + "/external-vocabularies/content");

    private AccessRules() {}

    /**
     * Returns the roles an operation needs.
     *
     * @param method HTTP method
     * @param path request path without query
     * @return the accepted roles, or null when no token is needed
     */
    public static Set<Role> required(String method, String path) {
        if (!path.equals(API_BASE) && !path.startsWith(API_BASE + "/")) {
            return null;
        }
        if (path.equals(API_BASE + "/health")) {
            return null;
        }
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return READ;
        }
        if ("POST".equals(method) && READ_ONLY_POSTS.contains(path)) {
            return READ;
        }
        return WRITE;
    }
}

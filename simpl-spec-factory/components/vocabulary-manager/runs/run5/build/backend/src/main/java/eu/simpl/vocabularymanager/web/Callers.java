package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.security.AuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;

/** Access to the caller the authentication filter stored on the request. */
public final class Callers {

    private Callers() {}

    /** The caller; secured operations always have one. */
    public static AuthenticatedUser of(HttpServletRequest request) {
        Object user = request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE);
        if (user instanceof AuthenticatedUser authenticated) {
            return authenticated;
        }
        throw ApiException.unauthorized("A valid bearer token is required.");
    }
}

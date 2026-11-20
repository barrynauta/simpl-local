package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.web.ApiOperation;
import eu.simpl.vocabularymanager.web.RoutingFilter;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Default-deny authorisation (D16, D84): each declared operation has its own role rule from {@link ApiOperation}; a
 * request that matches no operation is allowed only for an authenticated caller, who then gets 404 (D67).
 */
public class OperationAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        ApiOperation operation = RoutingFilter.operationOf(context.getRequest());
        return new AuthorizationDecision(isAllowed(operation, authentication.get()));
    }

    static boolean isAllowed(ApiOperation operation, Authentication authentication) {
        if (operation != null && operation.access() == ApiOperation.Access.PUBLIC) {
            return true;
        }
        if (!(authentication instanceof AuthenticatedUser user)) {
            return false;
        }
        return operation == null || user.hasAnyRole(operation.allowedRoles());
    }
}

package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.api.Routes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Default deny (D84): the service does not start when a handler exists without a role rule in
 * {@link Routes#ALL}, or a rule has no handler. Only the error page is exempt.
 */
@Component
public class RouteRuleCheck implements SmartInitializingSingleton {

    private final RequestMappingHandlerMapping mapping;

    public RouteRuleCheck(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public void afterSingletonsInstantiated() {
        check(mapping.getHandlerMethods());
    }

    /** Throws when handlers and rules differ. */
    public static void check(Map<RequestMappingInfo, HandlerMethod> handlers) {
        Set<String> declared = new HashSet<>();
        for (Routes.Route r : Routes.ALL) {
            declared.add(r.method() + " " + r.pattern());
        }
        Set<String> found = new HashSet<>();
        for (RequestMappingInfo info : handlers.keySet()) {
            Set<String> patterns = info.getPatternValues();
            if (patterns.contains("/error")) {
                continue;
            }
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                throw new IllegalStateException("Handler without method and role rule: " + patterns);
            }
            for (String p : patterns) {
                for (RequestMethod m : methods) {
                    String key = m.name() + " " + p;
                    if (!declared.contains(key)) {
                        throw new IllegalStateException("Handler without role rule: " + key);
                    }
                    found.add(key);
                }
            }
        }
        declared.removeAll(found);
        if (!declared.isEmpty()) {
            throw new IllegalStateException("Role rule without handler: " + declared);
        }
    }
}

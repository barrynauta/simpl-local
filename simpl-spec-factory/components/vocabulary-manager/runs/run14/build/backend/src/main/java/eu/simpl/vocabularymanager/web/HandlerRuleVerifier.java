package eu.simpl.vocabularymanager.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Default deny at start (D84, D206): the application fails to start when a Spring MVC handler has no row in the
 * operation table (and so no role rule), or a row has no handler.
 */
@Component
public class HandlerRuleVerifier {

    private final RequestMappingHandlerMapping handlerMapping;

    public HandlerRuleVerifier(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.handlerMapping = requestMappingHandlerMapping;
    }

    /** Runs the check once the context is up. */
    @EventListener(ApplicationStartedEvent.class)
    public void verify() {
        Set<String> handlers = handlerKeys(handlerMapping);
        Set<String> table = new HashSet<>();
        for (Operation operation : Operation.values()) {
            table.add(operation.method() + " " + operation.path());
        }
        Set<String> withoutRule = new HashSet<>(handlers);
        withoutRule.removeAll(table);
        Set<String> withoutHandler = new HashSet<>(table);
        withoutHandler.removeAll(handlers);
        if (!withoutRule.isEmpty() || !withoutHandler.isEmpty()) {
            throw new IllegalStateException("Handlers without a role rule: " + withoutRule
                    + "; operations without a handler: " + withoutHandler);
        }
    }

    /** All method and path pairs of the handler mapping. */
    public static Set<String> handlerKeys(RequestMappingHandlerMapping mapping) {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
                mapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                for (String pattern : info.getPatternValues()) {
                    keys.add(method.name() + " " + pattern);
                }
            }
        }
        return keys;
    }
}

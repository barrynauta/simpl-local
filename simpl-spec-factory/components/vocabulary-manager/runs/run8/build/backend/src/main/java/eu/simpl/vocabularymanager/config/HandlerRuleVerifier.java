package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.web.problem.ProblemErrorController;
import eu.simpl.vocabularymanager.web.routing.ApiOperation;
import eu.simpl.vocabularymanager.web.routing.OperationCatalogue;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * D84: the build fails at start when a handler exists without a role rule, or a rule has no handler. Compares every
 * MVC handler mapping with the operation catalogue.
 */
@Component
public class HandlerRuleVerifier {

    private final RequestMappingHandlerMapping handlerMapping;
    private final OperationCatalogue catalogue;

    /**
     * @param handlerMapping the MVC handler mapping
     * @param catalogue the operations and their role rules
     */
    public HandlerRuleVerifier(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            OperationCatalogue catalogue) {
        this.handlerMapping = handlerMapping;
        this.catalogue = catalogue;
    }

    /**
     * @param event the refresh event
     * @throws IllegalStateException when handlers and rules differ
     */
    @EventListener(ContextRefreshedEvent.class)
    public void verify(ContextRefreshedEvent event) {
        Set<String> problems = mismatches(handlerMapping.getHandlerMethods(), catalogue);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Handlers and role rules differ (D84): " + problems);
        }
    }

    /**
     * @param handlers the handler mappings
     * @param catalogue the operations
     * @return a description of every handler without a rule and every rule without a handler
     */
    static Set<String> mismatches(Map<RequestMappingInfo, HandlerMethod> handlers, OperationCatalogue catalogue) {
        Set<String> declared = new HashSet<>();
        for (ApiOperation operation : catalogue.operations()) {
            declared.add(operation.method().name() + " " + operation.path());
        }
        Set<String> served = new HashSet<>();
        Set<String> problems = new TreeSet<>();
        for (RequestMappingInfo info : handlers.keySet()) {
            for (String pattern : info.getPatternValues()) {
                if (ProblemErrorController.PATH.equals(pattern)) {
                    continue;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    problems.add("handler without method: " + pattern);
                }
                for (RequestMethod method : methods) {
                    String key = method.name() + " " + pattern;
                    served.add(key);
                    if (!declared.contains(key)) {
                        problems.add("handler without rule: " + key);
                    }
                }
            }
        }
        for (String key : declared) {
            if (!served.contains(key)) {
                problems.add("rule without handler: " + key);
            }
        }
        return problems;
    }
}

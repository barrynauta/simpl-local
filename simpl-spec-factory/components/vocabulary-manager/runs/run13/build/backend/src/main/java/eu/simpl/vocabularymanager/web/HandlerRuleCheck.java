package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Default deny (D84): the application refuses to start when a request handler has no access rule in the
 * operation catalog, or an operation of the catalog has no handler.
 */
public class HandlerRuleCheck implements ApplicationListener<ApplicationStartedEvent> {

    private final RequestMappingHandlerMapping mapping;

    public HandlerRuleCheck(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        List<String> problems = check(mapping.getHandlerMethods());
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Handlers without an access rule: " + problems);
        }
    }

    /** Returns one message per handler without a rule and per rule without a handler. */
    public static List<String> check(Map<RequestMappingInfo, HandlerMethod> handlers) {
        List<String> problems = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlers.entrySet()) {
            if (entry.getValue().getBeanType().equals(ProblemErrorController.class)) {
                continue;
            }
            RequestMappingInfo info = entry.getKey();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                if (methods.isEmpty()) {
                    problems.add("ANY " + pattern);
                }
                for (RequestMethod method : methods) {
                    if (OperationCatalog.byTemplate(method.name(), pattern).isEmpty()) {
                        problems.add(method + " " + pattern);
                    } else {
                        covered.add(method + " " + pattern);
                    }
                }
            }
        }
        for (OperationCatalog.Operation operation : OperationCatalog.operations()) {
            if (!covered.contains(operation.method() + " " + operation.template())) {
                problems.add("no handler for " + operation.operationId());
            }
        }
        return problems;
    }
}

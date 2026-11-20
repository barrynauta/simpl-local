package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Default deny (D84): the application fails to start when a handler exists without a route rule,
 * or a route rule has no handler.
 */
@Component
public class RouteRuleCheck implements SmartInitializingSingleton {

    private static final Set<String> INFRASTRUCTURE = Set.of("/error");

    private final RequestMappingHandlerMapping mapping;

    public RouteRuleCheck(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.mapping = requestMappingHandlerMapping;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> problems = check(mapping.getHandlerMethods().keySet());
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Handlers and route rules differ: " + problems);
        }
    }

    /** The differences between the handler mappings and the route table. */
    static List<String> check(Set<RequestMappingInfo> infos) {
        List<String> problems = new ArrayList<>();
        List<String> handled = new ArrayList<>();
        for (RequestMappingInfo info : infos) {
            for (String pattern : info.getPatternValues()) {
                if (INFRASTRUCTURE.contains(pattern)) {
                    continue;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    problems.add("handler without method: " + pattern);
                }
                for (RequestMethod method : methods) {
                    String key = method.name() + " " + pattern;
                    handled.add(key);
                    boolean ruled = ApiRoutes.all().stream()
                            .anyMatch(r -> (r.method() + " " + ApiRoutes.BASE + r.template()).equals(key));
                    if (!ruled) {
                        problems.add("handler without rule: " + key);
                    }
                }
            }
        }
        for (ApiRoutes.Route route : ApiRoutes.all()) {
            String key = route.method() + " " + ApiRoutes.BASE + route.template();
            if (!handled.contains(key)) {
                problems.add("rule without handler: " + key);
            }
        }
        return problems;
    }
}

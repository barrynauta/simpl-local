package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.web.ApiOperation;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * D84: every handler of the API has an explicit role rule in {@link ApiOperation}, and every rule has a handler.
 * The container error page is the only handler without a rule; it is reachable on ERROR dispatch only.
 */
class HandlerRulesTest extends IntegrationTestBase {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    @Test
    void everyHandlerHasARuleAndEveryRuleAHandler() {
        Set<String> handlers = new HashSet<>();
        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            for (String path : info.getPatternValues()) {
                if (path.equals("/error")) {
                    continue;
                }
                assertThat(info.getMethodsCondition().getMethods())
                        .as("handler %s declares its method", path)
                        .hasSize(1);
                info.getMethodsCondition().getMethods().forEach(method -> handlers.add(method.name() + " " + path));
            }
        }
        Set<String> rules = new HashSet<>();
        for (ApiOperation operation : ApiOperation.values()) {
            rules.add(operation.method() + " " + operation.pathTemplate());
            assertThat(operation.access()).isNotNull();
        }
        assertThat(handlers).isEqualTo(rules);
    }
}

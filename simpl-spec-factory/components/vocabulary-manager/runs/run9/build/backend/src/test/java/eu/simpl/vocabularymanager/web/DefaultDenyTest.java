package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** D84: every handler of the API has a route with an explicit role rule, and every route has a handler. */
class DefaultDenyTest extends ApiTestBase {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    @Autowired
    private RouteTable routes;

    @Test
    void everyHandlerHasARoleRule() {
        Set<String> handlers = new HashSet<>();
        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            for (String pattern : info.getPatternValues()) {
                if (pattern.equals("/error")) {
                    continue;
                }
                assertThat(info.getMethodsCondition().getMethods()).as(pattern).hasSize(1);
                String method = info.getMethodsCondition()
                        .getMethods()
                        .iterator()
                        .next()
                        .name();
                handlers.add(method + " " + pattern);
            }
        }
        Set<String> declared = new HashSet<>();
        for (Route route : routes.routes()) {
            declared.add(route.method() + " " + route.template());
            assertThat(route.roles().isEmpty()).isEqualTo(route.operationId().equals("getHealth"));
        }
        assertThat(handlers).isEqualTo(declared);
    }

    @Test
    void fixedSegmentsWin() {
        assertThat(routes.forPath("/v1/vocabularies/searches"))
                .extracting(Route::method)
                .containsExactly("POST");
        assertThat(routes.forPath("/v1/external-vocabularies/content"))
                .extracting(Route::operationId)
                .containsExactly("downloadExternalVocabularyContent");
        assertThat(routes.find("GET", "/v1/vocabularies/Abc/versions/1")).isPresent();
        assertThat(routes.forPath("no-slash")).isEmpty();
        assertThat(routes.forPath("/v1/vocabularies//versions")).isEmpty();
        assertThat(RouteTable.allow(routes.forPath("/v1/external-vocabularies/Abc")))
                .isEqualTo("DELETE, GET");
    }
}

package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

class HandlerRuleCheckTest {

    static class Rogue {
        public void handle() {}
    }

    @Test
    void handlerWithoutRuleIsReported() throws Exception {
        Method method = Rogue.class.getMethod("handle");
        RequestMappingInfo info = RequestMappingInfo.paths("/v1/secret")
                .methods(RequestMethod.GET)
                .build();
        var problems = HandlerRuleCheck.check(Map.of(info, new HandlerMethod(new Rogue(), method)));
        assertThat(problems).contains("GET /v1/secret");
        assertThat(problems).anyMatch(p -> p.startsWith("no handler for"));
        RequestMappingInfo any = RequestMappingInfo.paths("/v1/any").build();
        assertThat(HandlerRuleCheck.check(Map.of(any, new HandlerMethod(new Rogue(), method))))
                .contains("ANY /v1/any");
    }
}

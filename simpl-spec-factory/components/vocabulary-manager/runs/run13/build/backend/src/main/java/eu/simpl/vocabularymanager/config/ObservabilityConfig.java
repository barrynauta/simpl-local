package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.web.HandlerRuleCheck;
import eu.simpl.vocabularymanager.web.OperationObservationConvention;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Metrics naming and the start-up rule check. */
@Configuration
public class ObservabilityConfig {

    @Bean
    OperationObservationConvention operationObservationConvention() {
        return new OperationObservationConvention();
    }

    @Bean
    HandlerRuleCheck handlerRuleCheck(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        return new HandlerRuleCheck(mapping);
    }
}

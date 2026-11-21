package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.api.CorrelationFilter;
import eu.simpl.vocabularymanager.api.NegotiationFilter;
import eu.simpl.vocabularymanager.api.RouteGuardFilter;
import eu.simpl.vocabularymanager.api.Routes;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Filter order: tracing (framework) - correlation id - method check (405 before 401, D67) - Spring
 * Security (401, 403, firewall 400) - negotiation (406, 415, 413, D51/D63) - Spring MVC.
 */
@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> b = new FilterRegistrationBean<>(new CorrelationFilter());
        b.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return b;
    }

    @Bean
    FilterRegistrationBean<RouteGuardFilter> routeGuardFilter(ProblemWriter problems) {
        FilterRegistrationBean<RouteGuardFilter> b = new FilterRegistrationBean<>(new RouteGuardFilter(problems));
        b.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return b;
    }

    @Bean
    FilterRegistrationBean<NegotiationFilter> negotiationFilter(ProblemWriter problems) {
        FilterRegistrationBean<NegotiationFilter> b = new FilterRegistrationBean<>(new NegotiationFilter(problems));
        b.setOrder(0);
        return b;
    }

    /** Adds the OpenAPI operationId to the request metrics (D86). */
    @Bean
    DefaultServerRequestObservationConvention operationIdConvention() {
        return new DefaultServerRequestObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
                Object m = context.getCarrier().getAttribute(RouteGuardFilter.MATCH_ATTRIBUTE);
                String op = m instanceof Routes.Match match && match.route() != null
                        ? match.route().operationId()
                        : "none";
                return super.getLowCardinalityKeyValues(context).and(KeyValue.of("operationId", op));
            }
        };
    }
}

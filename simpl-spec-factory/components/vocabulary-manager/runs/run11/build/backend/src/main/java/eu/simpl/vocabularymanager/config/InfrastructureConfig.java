package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.store.StoreCallException;
import eu.simpl.vocabularymanager.web.CorrelationFilter;
import eu.simpl.vocabularymanager.web.MethodGuardFilter;
import eu.simpl.vocabularymanager.web.NegotiationFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Wiring of filters, outgoing HTTP clients, circuit breakers and the Tomcat connector. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class InfrastructureConfig {

    /** Spring Security's filter chain runs at order -100; these filters sit around it. */
    private static final int SECURITY_ORDER = -100;

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> bean = new FilterRegistrationBean<>(new CorrelationFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }

    @Bean
    FilterRegistrationBean<MethodGuardFilter> methodGuardFilter(ProblemWriter problems) {
        FilterRegistrationBean<MethodGuardFilter> bean = new FilterRegistrationBean<>(new MethodGuardFilter(problems));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }

    @Bean
    FilterRegistrationBean<NegotiationFilter> negotiationFilter(ProblemWriter problems, AppProperties properties) {
        FilterRegistrationBean<NegotiationFilter> bean =
                new FilterRegistrationBean<>(new NegotiationFilter(problems, properties));
        bean.setOrder(SECURITY_ORDER + 10);
        return bean;
    }

    /** Paths with encoded slashes reach the firewall, which answers 400 problem+json (D67). */
    @Bean
    TomcatConnectorCustomizer connectorCustomizer() {
        return connector -> {
            connector.setAllowTrace(true);
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
            connector.setProperty("relaxedPathChars", "[]|{}^`\"<>");
            connector.setProperty("relaxedQueryChars", "[]|{}^`\"<>");
        };
    }

    @Bean
    HttpClient storeHttpClient(AppProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.store().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    HttpClient identityHttpClient(AppProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.security().jwksTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** D103: open at 50 % failures of the last 20 calls (at least 10), 10 s open, 3 probes. */
    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meters) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .recordException(InfrastructureConfig::isFailure)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
        return registry;
    }

    @Bean
    CircuitBreaker storeCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("store");
    }

    @Bean
    CircuitBreaker identityCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("identityProvider");
    }

    /** A store 5xx and a timeout count; a store 4xx does not (D134). */
    static boolean isFailure(Throwable ex) {
        if (ex instanceof StoreCallException call) {
            return call.kind() != StoreCallException.Kind.CLIENT_ERROR;
        }
        return true;
    }
}

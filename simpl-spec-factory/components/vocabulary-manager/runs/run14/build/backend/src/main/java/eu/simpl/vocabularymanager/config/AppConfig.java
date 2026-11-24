package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.error.ProblemErrorReportValve;
import eu.simpl.vocabularymanager.store.StoreFailureException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.apache.catalina.core.StandardHost;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans of the component: clock, circuit breakers (D103) and the Tomcat settings of D158 and D169. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /** UTC clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Circuit breakers around the store and the key fetch (D103, D134): open when at least half of the last 20
     * calls failed (at least 10 calls), open for 10 s, then 3 probe calls. Store 4xx do not count.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordException(AppConfig::isBreakerFailure)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
        return registry;
    }

    static boolean isBreakerFailure(Throwable error) {
        if (error instanceof StoreFailureException failure) {
            return failure.serverError();
        }
        return error instanceof StoreUnavailableException || error instanceof java.io.IOException;
    }

    /**
     * Tomcat: encoded slashes reach the gatekeeper firewall (D67), errors before routing are problem+json (D158),
     * the server limits are above the service's own checks, and bodies of rejected requests are read to the end.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
                connector.setMaxPostSize(7 * 1024 * 1024);
                connector.setAllowTrace(false);
            });
            factory.addContextCustomizers(context -> {
                if (context.getParent() instanceof StandardHost host) {
                    host.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
                }
            });
        };
    }
}

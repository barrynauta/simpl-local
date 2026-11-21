package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.store.SparqlStoreClient;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Resilience4j set-up: circuit breakers (D103, D134), read retries (D44) and the upload bulkhead (D92). */
@Configuration
public class ResilienceConfig {

    public static final String STORE_BREAKER = "store";
    public static final String IDP_BREAKER = "identityProvider";

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordException(ResilienceConfig::isBreakerFailure)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
        return registry;
    }

    /** A timeout, a connection failure and a store 5xx count; a store 4xx does not (D134). */
    static boolean isBreakerFailure(Throwable error) {
        if (error instanceof SparqlStoreClient.StoreErrorException storeError) {
            return storeError.status() >= 500;
        }
        return true;
    }

    @Bean
    CircuitBreaker storeBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(STORE_BREAKER);
    }

    @Bean
    CircuitBreaker identityProviderBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(IDP_BREAKER);
    }

    /** Read retries: at most 2, after 200 ms and 400 ms, on connection failure or timeout only. */
    @Bean
    Retry storeReadRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(attempt -> attempt == 1 ? 200L : 400L)
                .retryOnException(error -> error instanceof SparqlStoreClient.StoreUnavailableException
                        && !(error.getCause()
                                instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException))
                .build();
        return Retry.of("storeRead", config);
    }

    /** At most this many create or new version requests per instance at the same time (D92). */
    @Bean
    Bulkhead uploadBulkhead(VocabularyManagerProperties properties) {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(properties.limits().maxParallelUploads())
                .maxWaitDuration(Duration.ZERO)
                .build();
        return Bulkhead.of("uploads", config);
    }
}

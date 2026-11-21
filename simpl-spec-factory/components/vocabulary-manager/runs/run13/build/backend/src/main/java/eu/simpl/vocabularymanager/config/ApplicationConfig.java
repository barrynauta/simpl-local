package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.api.Representations;
import eu.simpl.vocabularymanager.security.RealmKeyProvider;
import eu.simpl.vocabularymanager.security.TokenVerifier;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.service.WriteRetry;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.SparqlStoreClient;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.web.RequestGateFilter;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Wiring of the component (constructor injection everywhere, CODE-12, CODE-48). */
@Configuration
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TraceHeaders traceHeaders(ObjectProvider<Tracer> tracer, ObjectProvider<Propagator> propagator) {
        return new TraceHeaders(tracer.getIfAvailable(), propagator.getIfAvailable());
    }

    @Bean
    RealmKeyProvider realmKeyProvider(
            VocabularyManagerProperties properties,
            Clock clock,
            @Qualifier("identityProviderBreaker") CircuitBreaker breaker,
            TraceHeaders traceHeaders) {
        return new RealmKeyProvider(properties.identity(), clock, breaker, traceHeaders);
    }

    @Bean
    TokenVerifier tokenVerifier(VocabularyManagerProperties properties, RealmKeyProvider keys) {
        return new TokenVerifier(properties.identity(), keys);
    }

    /** Runs after the tracing filter, so trace ids are in the MDC, and before routing. */
    @Bean
    FilterRegistrationBean<RequestGateFilter> requestGateFilter(TokenVerifier verifier) {
        FilterRegistrationBean<RequestGateFilter> registration =
                new FilterRegistrationBean<>(new RequestGateFilter(verifier));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    SparqlStoreClient sparqlStoreClient(
            VocabularyManagerProperties properties,
            @Qualifier("storeBreaker") CircuitBreaker breaker,
            Retry storeReadRetry,
            TraceHeaders traceHeaders) {
        return new SparqlStoreClient(properties.store(), breaker, storeReadRetry, traceHeaders);
    }

    @Bean
    VocabularyRepository vocabularyRepository(SparqlStoreClient store) {
        return new VocabularyRepository(store);
    }

    @Bean
    ExternalVocabularyRepository externalVocabularyRepository(SparqlStoreClient store) {
        return new ExternalVocabularyRepository(store);
    }

    @Bean
    WriteRetry writeRetry(VocabularyManagerProperties properties, Clock clock) {
        return new WriteRetry(
                properties.limits().maxWriteAttempts(), properties.limits().writeBudget(), clock);
    }

    @Bean
    VocabularyService vocabularyService(
            VocabularyRepository repository, VocabularyManagerProperties properties, WriteRetry retry, Clock clock) {
        return new VocabularyService(repository, properties.contentBase(), retry, clock);
    }

    @Bean
    ExternalVocabularyService externalVocabularyService(ExternalVocabularyRepository repository, Clock clock) {
        return new ExternalVocabularyService(repository, clock);
    }

    @Bean
    Representations representations(VocabularyManagerProperties properties) {
        return new Representations(properties.contentBase());
    }
}

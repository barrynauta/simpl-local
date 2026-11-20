package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.api.Representations;
import eu.simpl.vocabularymanager.security.RealmKeyProvider;
import eu.simpl.vocabularymanager.service.ContentValidation;
import eu.simpl.vocabularymanager.service.ExternalVocabularyService;
import eu.simpl.vocabularymanager.service.SearchService;
import eu.simpl.vocabularymanager.service.UploadLimiter;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.SparqlStore;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ReferenceChecker;
import eu.simpl.vocabularymanager.validation.SemanticValidator;
import eu.simpl.vocabularymanager.validation.TurtleSyntaxValidator;
import eu.simpl.vocabularymanager.web.CorrelationFilter;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;

/** Wiring of the component: store, repositories, validation, services and the correlation filter. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ComponentConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Circuit breakers of D103: open at 50 % failures of the last 20 calls (at least 10), 10 s open, 3 probes. */
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
                .ignoreExceptions(SparqlStore.StoreFailureException.class)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
        registry.circuitBreaker("store");
        registry.circuitBreaker("realmKeys");
        return registry;
    }

    @Bean
    SparqlStore sparqlStore(AppProperties properties, CircuitBreakerRegistry breakers, OutboundHeaders headers) {
        AppProperties.Store settings = properties.store();
        return new SparqlStore(
                settings, SparqlStore.httpClient(settings.connectTimeout()), breakers.circuitBreaker("store"), headers);
    }

    @Bean
    VocabularyRepository vocabularyRepository(SparqlStore store) {
        return new VocabularyRepository(store);
    }

    @Bean
    ExternalVocabularyRepository externalVocabularyRepository(SparqlStore store) {
        return new ExternalVocabularyRepository(store);
    }

    @Bean
    TurtleSyntaxValidator turtleSyntaxValidator() {
        return new TurtleSyntaxValidator();
    }

    @Bean
    ContentValidation contentValidation(
            TurtleSyntaxValidator syntax, VocabularyRepository repository, AppProperties properties) {
        return new ContentValidation(
                syntax,
                new SemanticValidator(),
                new ReferenceChecker(properties.contentBase()),
                repository,
                properties.contentBase());
    }

    @Bean
    VocabularyService vocabularyService(VocabularyRepository repository, ContentValidation validation, Clock clock) {
        return new VocabularyService(repository, validation, clock);
    }

    @Bean
    ExternalVocabularyService externalVocabularyService(
            ExternalVocabularyRepository repository, TurtleSyntaxValidator syntax, Clock clock) {
        return new ExternalVocabularyService(repository, syntax, clock);
    }

    @Bean
    SearchService searchService(VocabularyRepository vocabularies, ExternalVocabularyRepository externals) {
        return new SearchService(vocabularies, externals);
    }

    @Bean
    UploadLimiter uploadLimiter(AppProperties properties) {
        return new UploadLimiter(properties.uploads().maxConcurrent());
    }

    @Bean
    Representations representations(AppProperties properties) {
        return new Representations(properties.contentBase());
    }

    @Bean
    FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>(new CorrelationFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /** Fetches the realm keys once at start, in the background; a failure only marks health DEGRADED (D108). */
    @EventListener(ApplicationReadyEvent.class)
    public void fetchRealmKeys(ApplicationReadyEvent event) {
        RealmKeyProvider keys = event.getApplicationContext().getBean(RealmKeyProvider.class);
        CompletableFuture.runAsync(keys::refresh);
    }
}

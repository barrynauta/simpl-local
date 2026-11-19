package eu.simpl.vocabulary.config;

import eu.simpl.vocabulary.api.ApiMapper;
import eu.simpl.vocabulary.domain.validation.TurtleValidator;
import eu.simpl.vocabulary.service.ContentValidation;
import eu.simpl.vocabulary.service.ExternalVocabularyService;
import eu.simpl.vocabulary.service.NameLocks;
import eu.simpl.vocabulary.service.SearchService;
import eu.simpl.vocabulary.service.VocabularyService;
import eu.simpl.vocabulary.store.FusekiVocabularyStore;
import eu.simpl.vocabulary.store.SparqlClient;
import eu.simpl.vocabulary.store.VocabularyStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring of store, validation and services (dependency injection, CODE-12). */
@Configuration
public class ServiceConfig {

    @Bean
    public SparqlClient sparqlClient(AppProperties properties) {
        return new SparqlClient(properties.store(), duration -> Thread.sleep(duration));
    }

    @Bean
    public VocabularyStore vocabularyStore(SparqlClient client) {
        return new FusekiVocabularyStore(client);
    }

    @Bean
    public ContentValidation contentValidation(VocabularyStore store, AppProperties properties) {
        return new ContentValidation(new TurtleValidator(), store, properties.contentBaseUrl());
    }

    @Bean
    public NameLocks nameLocks(AppProperties properties) {
        return new NameLocks(properties.limits().lockTimeout());
    }

    @Bean
    public VocabularyService vocabularyService(
            VocabularyStore store, ContentValidation validation, NameLocks locks, Clock clock) {
        return new VocabularyService(store, validation, locks, clock);
    }

    @Bean
    public ExternalVocabularyService externalVocabularyService(
            VocabularyStore store, ContentValidation validation, NameLocks locks, Clock clock) {
        return new ExternalVocabularyService(store, validation, locks, clock);
    }

    @Bean
    public SearchService searchService(VocabularyStore store) {
        return new SearchService(store);
    }

    @Bean
    public ApiMapper apiMapper(AppProperties properties) {
        return new ApiMapper(properties.contentBaseUrl());
    }
}

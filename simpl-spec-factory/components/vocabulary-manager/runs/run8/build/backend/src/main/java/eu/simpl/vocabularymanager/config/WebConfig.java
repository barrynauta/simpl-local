package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.store.HttpSparqlStore;
import eu.simpl.vocabularymanager.store.SparqlStore;
import eu.simpl.vocabularymanager.web.problem.ProblemWriter;
import eu.simpl.vocabularymanager.web.routing.OperationCatalogue;
import eu.simpl.vocabularymanager.web.routing.RequestShapeInterceptor;
import eu.simpl.vocabularymanager.web.routing.RoutingGuardFilter;
import eu.simpl.vocabularymanager.web.trace.CorrelationFilter;
import java.net.http.HttpClient;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web wiring: API base /v1 (E2), filter order of D67, request-shape interceptor, store client. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OperationCatalogue catalogue;

    /**
     * @param catalogue the operations of the contract
     */
    public WebConfig(OperationCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(OperationCatalogue.API_BASE, HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestShapeInterceptor(catalogue));
    }

    /**
     * @return the correlation and access log filter, first of all filters
     */
    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>(new CorrelationFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * @param problemWriter writes the 405 body
     * @return the 405 guard, before the firewall and the token check (D67)
     */
    @Bean
    public FilterRegistrationBean<RoutingGuardFilter> routingGuardFilter(ProblemWriter problemWriter) {
        FilterRegistrationBean<RoutingGuardFilter> registration =
                new FilterRegistrationBean<>(new RoutingGuardFilter(catalogue, problemWriter));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    /**
     * @param properties component settings
     * @return the HTTP client for the store and the identity provider: connect timeout of D44, no redirects
     *     (OWA-API7)
     */
    @Bean
    public HttpClient httpClient(VocabularyManagerProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.store().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * @param httpClient the HTTP client
     * @param properties component settings
     * @return the store client
     */
    @Bean
    public SparqlStore sparqlStore(HttpClient httpClient, VocabularyManagerProperties properties) {
        return new HttpSparqlStore(httpClient, properties.store());
    }
}

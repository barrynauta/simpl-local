package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.Representations;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.store.OutboundHeaders;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.catalina.core.StandardHost;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Web wiring: filters in the decided order (D67), Tomcat settings, clock and trace forwarding (D86). */
@Configuration
public class WebConfig {

    /** @return the UTC clock */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * @param properties component configuration
     * @return wire representations
     */
    @Bean
    public Representations representations(VocabularyManagerProperties properties) {
        return new Representations(properties.contentBase());
    }

    /** @return the correlation filter, right after the tracing filter */
    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>(new CorrelationFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    /**
     * @param routes route table
     * @return the 405 filter, before the security filters
     */
    @Bean
    public FilterRegistrationBean<RouteFilter> routeFilter(RouteTable routes) {
        FilterRegistrationBean<RouteFilter> registration = new FilterRegistrationBean<>(new RouteFilter(routes));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * Lets encoded slashes reach the firewall (400 problem, D67), raises the part limit for 20 aliases (D56) and
     * writes container errors as problem+json.
     *
     * @return the Tomcat customizer
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setEncodedSolidusHandling("passthrough");
                connector.setMaxPartCount(64);
                connector.setAllowTrace(false);
            });
            factory.addContextCustomizers(context -> {
                if (context.getParent() instanceof StandardHost host) {
                    host.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
                }
            });
        };
    }

    /**
     * Forwards traceparent and X-Correlation-Id on calls to the store and the identity provider (D86).
     *
     * @param tracer the tracer, when tracing is on
     * @param propagator the W3C propagator, when tracing is on
     * @return the header source
     */
    @Bean
    public OutboundHeaders outboundHeaders(ObjectProvider<Tracer> tracer, ObjectProvider<Propagator> propagator) {
        return () -> {
            Map<String, String> headers = new LinkedHashMap<>();
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                headers.put(CorrelationFilter.HEADER, correlationId);
            }
            Tracer currentTracer = tracer.getIfAvailable();
            Propagator currentPropagator = propagator.getIfAvailable();
            if (currentTracer != null && currentPropagator != null && currentTracer.currentSpan() != null) {
                currentPropagator.inject(
                        currentTracer.currentSpan().context(),
                        headers,
                        (carrier, key, value) -> carrier.put(key, value));
            }
            return headers;
        };
    }
}

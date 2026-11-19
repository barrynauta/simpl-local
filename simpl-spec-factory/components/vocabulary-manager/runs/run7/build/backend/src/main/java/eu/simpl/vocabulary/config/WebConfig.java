package eu.simpl.vocabulary.config;

import eu.simpl.vocabulary.api.filter.EntryFilter;
import eu.simpl.vocabulary.api.filter.RequestGateFilter;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;

/** Servlet filters around Spring Security and shared beans. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig {

    /** Runs before Spring Security (order -100). */
    static final int ENTRY_FILTER_ORDER = -1000;

    /** Runs after Spring Security. */
    static final int GATE_FILTER_ORDER = -50;

    @Bean
    public FilterRegistrationBean<EntryFilter> entryFilter(
            HttpFirewall firewall,
            ProblemWriter problemWriter,
            @Value("${management.server.port:8081}") int managementPort) {
        FilterRegistrationBean<EntryFilter> registration =
                new FilterRegistrationBean<>(new EntryFilter(firewall, problemWriter, managementPort));
        registration.setOrder(ENTRY_FILTER_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestGateFilter> requestGateFilter(
            ProblemWriter problemWriter, AppProperties properties) {
        FilterRegistrationBean<RequestGateFilter> registration =
                new FilterRegistrationBean<>(new RequestGateFilter(problemWriter, properties.limits()));
        registration.setOrder(GATE_FILTER_ORDER);
        return registration;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

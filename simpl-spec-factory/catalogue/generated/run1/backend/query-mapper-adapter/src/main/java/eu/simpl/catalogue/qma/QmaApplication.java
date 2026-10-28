package eu.simpl.catalogue.qma;

import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.SecurityProps;
import eu.simpl.catalogue.common.SecuritySupport;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/** Query mapper adapter: the Governance Authority search and lookup entry point (D4, D36). */
@SpringBootApplication(scanBasePackages = {"eu.simpl.catalogue.common", "eu.simpl.catalogue.qma"})
@EnableConfigurationProperties({SecurityProps.class, QmaProps.class})
public class QmaApplication {
    public static void main(String[] args) {
        SpringApplication.run(QmaApplication.class, args);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, SecurityProps props) throws Exception {
        return SecuritySupport.chain(http, props, List.of("/health", "/error"));
    }

    @Bean
    FederationConfig federationConfig(QmaProps props) {
        return FederationConfig.load(props.getFederationFile());
    }

    /** D48: the Trustor echoes Correlation-Id on every /federated response, errors included. */
    @Bean
    FilterRegistrationBean<Filter> correlationIdFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>((req, res, chain) -> {
            HttpServletRequest r = (HttpServletRequest) req;
            String cid = r.getHeader("Correlation-Id");
            if (cid != null && r.getRequestURI().startsWith("/v1/federated/")) {
                ((HttpServletResponse) res).setHeader("Correlation-Id", cid);
            }
            chain.doFilter(req, res);
        });
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }
}

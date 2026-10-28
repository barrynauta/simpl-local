package eu.simpl.catalogue.cbe;

import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.SecurityProps;
import eu.simpl.catalogue.common.SecuritySupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/** Catalogue client backend (xfsc-advsearch-be): the consumer-facing backend of the UI. */
@SpringBootApplication(scanBasePackages = {"eu.simpl.catalogue.common", "eu.simpl.catalogue.cbe"})
@EnableConfigurationProperties({SecurityProps.class, CbeProps.class})
public class CbeApplication {
    public static void main(String[] args) {
        SpringApplication.run(CbeApplication.class, args);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, SecurityProps props) throws Exception {
        return SecuritySupport.chain(http, props, List.of("/health", "/error"));
    }

    @Bean
    FederationConfig federationConfig(CbeProps props) {
        return FederationConfig.load(props.getFederationFile());
    }
}

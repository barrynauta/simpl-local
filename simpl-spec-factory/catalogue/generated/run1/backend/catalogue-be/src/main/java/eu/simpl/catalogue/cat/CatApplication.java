package eu.simpl.catalogue.cat;

import eu.simpl.catalogue.cat.crypto.KeyRegistry;
import eu.simpl.catalogue.common.SecurityProps;
import eu.simpl.catalogue.common.SecuritySupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/** catalogue-be: the federated catalogue of one data space. */
@SpringBootApplication(scanBasePackages = {"eu.simpl.catalogue.common", "eu.simpl.catalogue.cat"})
@EnableConfigurationProperties({SecurityProps.class, CatProps.class})
public class CatApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatApplication.class, args);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, SecurityProps props) throws Exception {
        return SecuritySupport.chain(http, props, List.of("/health", "/webhooks/schema-events", "/query", "/session",
                "/error"));
    }

    @Bean
    KeyRegistry keyRegistry(CatProps props) {
        return KeyRegistry.load(props.getKeyRegistry());
    }
}

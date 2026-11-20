package eu.simpl.vocabularymanager.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lets encoded slashes reach the Spring Security firewall, which answers with a problem+json 400 (D67), instead of
 * Tomcat's own HTML error page.
 */
@Configuration
public class TomcatConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> encodedSlashes() {
        return factory ->
                factory.addConnectorCustomizers(connector -> connector.setEncodedSolidusHandling("passthrough"));
    }
}

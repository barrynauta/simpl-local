package eu.simpl.vocabularymanager.web;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardHost;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tomcat settings through its extension points: problem+json error pages and request part limits (NFR-04). */
@Configuration
public class WebServerConfig {

    /**
     * Customises Tomcat.
     *
     * @return the customiser
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addContextCustomizers(context -> {
                if (context.getParent() instanceof Host host && host instanceof StandardHost standardHost) {
                    standardHost.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
                }
            });
            factory.addConnectorCustomizers(connector -> {
                connector.setMaxPartCount(64);
                connector.setEncodedSolidusHandling("passthrough");
            });
        };
    }
}

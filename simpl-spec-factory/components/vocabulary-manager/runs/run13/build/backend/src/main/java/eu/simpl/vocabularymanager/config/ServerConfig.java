package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.web.ProblemErrorReportValve;
import org.apache.catalina.core.StandardHost;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet container settings (D158, D166): problem details from the container's own error answers,
 * encoded slashes passed on to the firewall of the request gate (400 problem), TRACE refused.
 */
@Configuration
public class ServerConfig {

    @Bean
    TomcatContextCustomizer problemErrorValve() {
        return context -> {
            if (context.getParent() instanceof StandardHost host) {
                host.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
            }
        };
    }

    @Bean
    TomcatConnectorCustomizer connectorSettings() {
        return connector -> {
            connector.setAllowTrace(false);
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
            connector.setXpoweredBy(false);
        };
    }
}

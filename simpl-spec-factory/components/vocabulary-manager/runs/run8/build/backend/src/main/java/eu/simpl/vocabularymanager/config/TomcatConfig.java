package eu.simpl.vocabularymanager.config;

import java.io.IOException;
import java.io.Writer;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat settings for D67: an encoded slash reaches the Spring Security firewall (which refuses it with the Belgif 400
 * of D67) instead of being refused by Tomcat with an HTML page, and any error Tomcat itself answers before the
 * application runs (bad request line, control characters) has an application/problem+json body.
 */
@Configuration
public class TomcatConfig {

    /**
     * @return the customizer of the embedded Tomcat
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> problemTomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> connector.setEncodedSolidusHandling("passthrough"));
            factory.addContextCustomizers(context -> {
                Container parent = context.getParent();
                if (parent instanceof StandardHost host) {
                    host.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
                }
            });
        };
    }

    /** Replaces Tomcat's HTML error page with a minimal problem+json body. */
    public static class ProblemErrorReportValve extends ErrorReportValve {

        @Override
        protected void report(Request request, Response response, Throwable throwable) {
            int status = response.getStatus();
            if (status < 400 || response.getContentWritten() > 0) {
                return;
            }
            String body = problemBody(status);
            try {
                response.setContentType("application/problem+json");
                response.setCharacterEncoding("UTF-8");
                Writer writer = response.getReporter();
                if (writer != null) {
                    writer.write(body);
                    response.finishResponse();
                }
            } catch (IOException | IllegalStateException e) {
                // The client is gone or the response is already committed; nothing more can be sent.
            }
        }

        /**
         * @param status the HTTP status
         * @return the problem body Tomcat sends for that status
         */
        static String problemBody(int status) {
            if (status == 400) {
                return "{\"type\":\"urn:problem-type:belgif:badRequest\","
                        + "\"href\":\"https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html\","
                        + "\"title\":\"Bad Request\",\"status\":400,\"detail\":\"The input message is incorrect\"}";
            }
            if (status == 413) {
                return "{\"type\":\"urn:problem-type:simpl:payloadTooLarge\",\"title\":\"Payload Too Large\","
                        + "\"status\":413,\"detail\":\"Uploaded vocabulary file is too large. The maximum allowed"
                        + " upload size is 5 MB. Please upload a smaller Turtle file.\"}";
            }
            if (status >= 500) {
                return "{\"type\":\"urn:problem-type:belgif:internalServerError\","
                        + "\"href\":\"https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html\","
                        + "\"title\":\"Internal Server Error\",\"status\":" + status
                        + ",\"detail\":\"An unexpected error occurred.\"}";
            }
            return "{\"type\":\"about:blank\",\"status\":" + status + "}";
        }
    }
}

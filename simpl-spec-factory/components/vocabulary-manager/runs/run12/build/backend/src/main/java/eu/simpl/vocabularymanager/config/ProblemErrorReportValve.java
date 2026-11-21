package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.error.ProblemType;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Requests the servlet container refuses before routing (bad encoding, encoded slash, too many
 * parameters) are answered with problem+json too (D158), not with the container's HTML page.
 */
public class ProblemErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable t) {
        int status = response.getStatus();
        if (status < 400 || response.getContentWritten() > 0) {
            return;
        }
        ProblemType p = ProblemType.forStatus(status);
        String detail = status == 400 ? "The request was rejected by the server." : p.title();
        String href = p.href() == null ? "" : ",\"href\":\"" + p.href() + "\"";
        String body = "{\"type\":\""
                + p.type()
                + "\""
                + href
                + ",\"title\":\""
                + p.title()
                + "\",\"status\":"
                + status
                + ",\"detail\":\""
                + detail
                + "\",\"instance\":\"urn:uuid:"
                + UUID.randomUUID()
                + "\"}";
        try {
            response.setContentType("application/problem+json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            OutputStream out = response.getOutputStream();
            out.write(bytes);
            response.finishResponse();
        } catch (IOException | IllegalStateException e) {
            // The client is gone; nothing more to do.
        }
    }

    /** Replaces the host's error report valve once the server runs. */
    @Component
    public static class Installer implements ApplicationListener<WebServerInitializedEvent> {
        @Override
        public void onApplicationEvent(WebServerInitializedEvent event) {
            if (event.getWebServer() instanceof TomcatWebServer tomcat) {
                var host = tomcat.getTomcat().getHost();
                for (Valve v : host.getPipeline().getValves()) {
                    if (v instanceof ErrorReportValve && !(v instanceof ProblemErrorReportValve)) {
                        host.getPipeline().removeValve(v);
                    }
                }
                boolean present = false;
                for (Valve v : host.getPipeline().getValves()) {
                    present |= v instanceof ProblemErrorReportValve;
                }
                if (!present) {
                    host.getPipeline().addValve(new ProblemErrorReportValve());
                }
            }
        }
    }
}

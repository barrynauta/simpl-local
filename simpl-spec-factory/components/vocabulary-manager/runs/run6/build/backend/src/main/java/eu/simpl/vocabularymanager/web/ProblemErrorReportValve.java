package eu.simpl.vocabularymanager.web;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/**
 * Tomcat error report valve that writes problem+json instead of an HTML page, for errors Tomcat answers before
 * the request reaches Spring (Tomcat extension point, REUSE-09).
 */
public class ProblemErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if (status < 400 || response.isCommitted() || response.getContentWritten() > 0) {
            return;
        }
        boolean client = status < 500;
        String type = client ? "urn:problem-type:belgif:badRequest" : "urn:problem-type:belgif:internalServerError";
        String title = client ? "Bad Request" : "Internal Server Error";
        String detail = client ? "The input message is incorrect" : "Internal Server Error";
        String body = "{\"type\":\"" + type + "\",\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\""
                + detail + "\",\"instance\":\"urn:uuid:" + UUID.randomUUID() + "\"}";
        try {
            response.setContentType("application/problem+json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Writer writer = response.getReporter();
            if (writer != null) {
                writer.write(body);
                response.finishResponse();
            }
        } catch (IOException | IllegalStateException e) {
            container.getLogger().warn("Could not write the error body", e);
        }
    }
}

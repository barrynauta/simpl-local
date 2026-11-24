package eu.simpl.vocabularymanager.error;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/**
 * Tomcat error report valve that renders errors the container produces before routing (bad encoding, malformed
 * requests, too large headers) as problem+json (D158) instead of an HTML page.
 */
public class ProblemErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if (status < 400 || response.isCommitted() || response.getContentWritten() > 0) {
            return;
        }
        ProblemType type = ProblemType.forStatus(status);
        Problem problem = new Problem(
                type.uri(),
                type.href(),
                type.title(),
                status,
                type.defaultDetail(),
                "urn:uuid:" + UUID.randomUUID(),
                null,
                null);
        byte[] body = ProblemWriter.toBytes(problem);
        try {
            response.setContentType(ProblemWriter.PROBLEM_JSON);
            response.setContentLength(body.length);
            OutputStream output = response.getOutputStream();
            output.write(body);
            output.flush();
        } catch (IOException | IllegalStateException e) {
            container.getLogger().debug("Problem body could not be written", e);
        }
    }
}

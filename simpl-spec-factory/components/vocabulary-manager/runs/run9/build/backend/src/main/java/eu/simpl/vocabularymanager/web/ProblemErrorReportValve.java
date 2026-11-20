package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.ProblemWriter;
import eu.simpl.vocabularymanager.api.Problems;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/** Container-level errors (for example a malformed request line) as problem+json instead of an HTML page. */
public class ProblemErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        try {
            byte[] body = ProblemWriter.body(Problems.generic(status, "HTTP " + status));
            response.setContentType(ProblemWriter.PROBLEM_JSON);
            response.setContentLength(body.length);
            OutputStream out = response.getOutputStream();
            out.write(body);
            out.flush();
        } catch (IOException | IllegalStateException ex) {
            container.getLogger().warn("Could not write the error response", ex);
        }
    }
}

package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/**
 * Tomcat error report valve that writes problem details instead of HTML (D158). It covers answers the
 * container gives before the application sees the request (bad encoding, too many parameters, TRACE).
 */
public class ProblemErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        ProblemType type = ProblemType.forStatus(status);
        String detail = status >= 500 ? "Internal Server Error" : type.title();
        byte[] body = ProblemWriter.bytes(new ApiException(type, detail));
        try {
            response.setContentType(ProblemWriter.PROBLEM_JSON);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getOutputStream().write(body);
            response.finishResponse();
        } catch (IOException | IllegalStateException e) {
            // The client went away; nothing more can be sent.
        }
    }
}

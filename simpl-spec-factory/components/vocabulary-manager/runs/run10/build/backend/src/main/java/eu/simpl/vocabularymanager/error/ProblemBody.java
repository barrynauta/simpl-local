package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/** The RFC 9457 body written for every problem. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemBody(
        String type, String href, String title, int status, String detail, String instance, List<Issue> issues) {

    public static ProblemBody of(ProblemException problem) {
        return new ProblemBody(
                problem.type(),
                problem.href(),
                problem.title(),
                problem.status(),
                problem.detail(),
                "urn:uuid:" + UUID.randomUUID(),
                problem.issues().isEmpty() ? null : problem.issues());
    }
}

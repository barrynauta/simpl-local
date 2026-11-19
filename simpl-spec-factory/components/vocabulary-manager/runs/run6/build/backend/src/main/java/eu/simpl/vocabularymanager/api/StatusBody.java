package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.problem.Issue;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** VocabularyStatusPatchRequest: exactly one property, status, ACTIVE or DEPRECATED (decisions D4, D52). */
final class StatusBody {

    private StatusBody() {}

    static VocabularyStatus read(JsonNode body, List<Issue> issues) {
        if (!JsonBodyReader.checkObject(body, "", Set.of("status"), issues)) {
            return null;
        }
        JsonNode status = body.get("status");
        if (status == null) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "status", "Field 'status' is required."));
            return null;
        }
        if (!status.isString()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "status", "Field 'status' must be one of [ACTIVE, DEPRECATED]."));
            return null;
        }
        return VocabularyStatus.fromWire(status.asString()).orElseGet(() -> {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, "status", "Field 'status' must be one of [ACTIVE, DEPRECATED]."));
            return null;
        });
    }
}

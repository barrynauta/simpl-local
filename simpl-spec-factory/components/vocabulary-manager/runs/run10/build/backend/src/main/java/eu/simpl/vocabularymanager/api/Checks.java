package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.request.FieldRules;
import eu.simpl.vocabularymanager.request.QueryParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.MDC;

/** Collects the 400 issues of one request (path, query, body) and throws them together (D49, D51, D52). */
final class Checks {

    final List<Issue> issues = new ArrayList<>();

    QueryParameters query(HttpServletRequest request, Set<String> allowed) {
        QueryParameters parameters = QueryParameters.parse(request.getQueryString(), allowed);
        issues.addAll(parameters.issues());
        return parameters;
    }

    /** No query parameter is allowed (POST, PATCH, DELETE and GET without parameters). */
    void noQuery(HttpServletRequest request) {
        query(request, Set.of());
    }

    String pathName(String value, String parameter) {
        if (!FieldRules.isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    "path",
                    parameter,
                    "'" + parameter + "' must be 3 to 64 characters, start with an upper-case letter and contain"
                            + " only letters and digits.",
                    value));
            return null;
        }
        MDC.put("vocabularyName", value);
        return value;
    }

    Integer pathVersion(String value) {
        Integer version = FieldRules.positiveInt(value, "path", "version", Integer.MAX_VALUE, issues);
        if (version != null) {
            MDC.put("version", version.toString());
        }
        return version;
    }

    void throwIfAny() {
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
    }
}

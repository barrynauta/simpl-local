package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.validation.Names;
import eu.simpl.vocabularymanager.validation.QueryParameters;
import eu.simpl.vocabularymanager.validation.UriRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.MDC;
import tools.jackson.databind.JsonNode;

/** Collects the 400 issues of one request (D49, D51, D52, D37) and throws them together. */
public final class RequestChecks {

    private final List<Issue> issues = new ArrayList<>();

    public static RequestChecks start() {
        return new RequestChecks();
    }

    /** A vocabulary or external vocabulary name in the path (D49). */
    public RequestChecks pathName(String parameter, String value) {
        if (!Names.isVocabularyName(value)) {
            issues.add(Issue.schema(
                    "path",
                    parameter,
                    "Path parameter '" + parameter + "' must match ^[A-Z][a-zA-Z0-9]*$ (3 to 64 characters)."));
        } else if ("vocabularyName".equals(parameter) || "name".equals(parameter)) {
            MDC.put("vocabularyName", value);
        }
        return this;
    }

    /** A version in the path: digits only, 1 to 2147483647 (D49, D91, D112). Returns -1 when invalid. */
    public int pathVersion(String value) {
        long parsed = Names.positiveInt(value, Integer.MAX_VALUE);
        if (parsed < 0) {
            issues.add(Issue.schema(
                    "path", "version", "Path parameter 'version' must be an integer from 1 to 2147483647."));
            return -1;
        }
        MDC.put("version", Long.toString(parsed));
        return (int) parsed;
    }

    /** No query parameters on POST, PATCH and DELETE (D52). */
    public RequestChecks noQuery(String rawQuery) {
        try {
            QueryParameters.parse(rawQuery, Set.of());
        } catch (ApiException ex) {
            issues.addAll(ex.issues());
        }
        return this;
    }

    public RequestChecks add(Issue issue) {
        issues.add(issue);
        return this;
    }

    public RequestChecks addAll(List<Issue> more) {
        issues.addAll(more);
        return this;
    }

    /** Required text, 1 to 1000 characters after trimming (D37). Returns the trimmed value. */
    public String requiredText(String field, String value) {
        if (value == null) {
            issues.add(Issue.schema("body", field, "Field '" + field + "' is required."));
            return null;
        }
        int length = Names.trimmedLength(value);
        if (length < 1 || length > Names.TEXT_MAX) {
            issues.add(Issue.schema(
                    "body", field, "Field '" + field + "' must have 1 to 1000 characters after trimming."));
            return null;
        }
        return value.strip();
    }

    /** Optional text, at most 1000 characters after trimming (D56). Empty counts as absent. */
    public String optionalText(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (Names.trimmedLength(value) > Names.TEXT_MAX) {
            issues.add(Issue.schema(
                    "body", field, "Field '" + field + "' must have at most 1000 characters after trimming."));
            return null;
        }
        return value.strip();
    }

    /** A vocabulary name field (D37). */
    public String name(String field, String value) {
        if (value == null) {
            issues.add(Issue.schema("body", field, "Field '" + field + "' is required."));
            return null;
        }
        if (!Names.isVocabularyName(value)) {
            issues.add(Issue.schema(
                    "body", field, "Field '" + field + "' must match ^[A-Z][a-zA-Z0-9]*$ (3 to 64 characters)."));
            return null;
        }
        return value;
    }

    /** An absolute URI field (D73). */
    public String uri(String field, String value, boolean required) {
        if (value == null) {
            if (required) {
                issues.add(Issue.schema("body", field, "Field '" + field + "' is required."));
            }
            return null;
        }
        if (!UriRules.isAbsoluteUri(value)) {
            issues.add(Issue.schema("body", field, "Field '" + field + "' must be an absolute URI."));
            return null;
        }
        return value;
    }

    /** The status of a PATCH body (D4, D52). */
    public VocabularyStatus status(JsonNode body) {
        for (String unknown : JsonBody.unknownProperties(body, Set.of("status"))) {
            issues.add(Issue.schema("body", unknown, "Property '" + unknown + "' is not supported."));
        }
        JsonNode value = body.get("status");
        if (value == null) {
            issues.add(Issue.schema("body", "status", "Property 'status' is required."));
            return null;
        }
        if (!value.isString() || VocabularyStatus.parse(value.asString()).isEmpty()) {
            issues.add(Issue.schema("body", "status", "Property 'status' must be one of [ACTIVE, DEPRECATED]."));
            return null;
        }
        return VocabularyStatus.parse(value.asString()).orElseThrow();
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Throws the collected issues.
     *
     * @throws ApiException 400 when there is at least one issue
     */
    public void throwIfAny() {
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
    }
}

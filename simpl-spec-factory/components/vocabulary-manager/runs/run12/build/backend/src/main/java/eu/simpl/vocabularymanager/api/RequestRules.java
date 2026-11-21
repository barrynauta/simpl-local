package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.security.TokenVerifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.JsonNode;

/** Shared request checks of the controllers (D7, D37, D52). */
final class RequestRules {

    static final Set<String> NONE = Set.of();

    private RequestRules() {}

    /** Any query parameter on POST, PATCH or DELETE is 400 (D52). */
    static void noQuery(String rawQuery) {
        QueryParams.parse(rawQuery, NONE);
    }

    static void throwIfAny(List<Issue> issues) {
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
    }

    /** A required text field of 1 to 1000 characters after trimming (D37). */
    static void requiredText(MultipartForm form, String field, List<Issue> issues) {
        if (!form.has(field)) {
            issues.add(Issue.schemaViolation("body", field, "Field '" + field + "' is required.", null));
        } else if (form.oversized(field) || !Validation.validText(form.text(field))) {
            issues.add(Issue.schemaViolation(
                    "body", field, "Field '" + field + "' must be 1 to 1000 characters after trimming.", null));
        }
    }

    static void requiredFile(MultipartForm form, List<Issue> issues) {
        if (form.file() == null) {
            issues.add(Issue.schemaViolation("body", MultipartForm.FILE, "Field 'vocabularyFile' is required.", null));
        }
    }

    static void name(String value, boolean present, List<Issue> issues) {
        if (!present) {
            issues.add(Issue.schemaViolation("body", "name", "Field 'name' is required.", null));
        } else if (!Validation.validName(value)) {
            issues.add(Issue.schemaViolation(
                    "body",
                    "name",
                    "Field 'name' must be 3 to 64 characters matching ^[A-Z][a-zA-Z0-9]*$.",
                    value != null && value.length() <= 100 ? value : null));
        }
    }

    /** Unknown properties of a JSON object, each an issue named with its path (D52). */
    static void unknownProperties(JsonNode obj, Set<String> allowed, String prefix, List<Issue> out) {
        for (String name : obj.propertyNames()) {
            if (!allowed.contains(name)) {
                String path = prefix.isEmpty() ? name : prefix + "." + name;
                out.add(Issue.schemaViolation("body", path, "Property '" + path + "' is not supported.", null));
            }
        }
    }

    static List<Issue> issues() {
        return new ArrayList<>();
    }

    /** The verified user of the request. */
    static TokenVerifier.Principal user() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (TokenVerifier.Principal) a.getPrincipal();
    }
}

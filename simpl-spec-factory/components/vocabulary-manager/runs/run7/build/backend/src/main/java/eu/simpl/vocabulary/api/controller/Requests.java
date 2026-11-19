package eu.simpl.vocabulary.api.controller;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.api.request.MultipartForm;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/** Shared request checks and response helpers of the controllers. */
final class Requests {

    static final MediaType TURTLE = new MediaType("text", "turtle");

    private Requests() {}

    /** Throws 400 when issues were found (D51: all request-shape issues in one response). */
    static void rejectIfAny(List<Issue> issues) {
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
    }

    /** Checks a vocabulary name path value (D49). */
    static void checkPathName(String parameter, String value, List<Issue> issues) {
        if (!FieldRules.isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_PATH,
                    parameter,
                    "Path parameter '" + parameter
                            + "' must be 3 to 64 letters or digits starting with an upper-case letter."));
        }
    }

    /** Parses a version path value (D49). */
    static long checkPathVersion(String value, List<Issue> issues) {
        Optional<Long> version = FieldRules.parseVersion(value);
        if (version.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_PATH, "version", "Path parameter 'version' must be a positive integer."));
            return -1;
        }
        return version.get();
    }

    /**
     * Reads the status of a status PATCH body (D4, D52).
     *
     * @param body the JSON body
     * @param issues issue sink
     * @return the status, null when invalid
     */
    static VocabularyStatus statusBody(JsonNode body, List<Issue> issues) {
        if (body == null) {
            return null;
        }
        if (!body.isObject()) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "body", "The body must be a JSON object."));
            return null;
        }
        for (Map.Entry<String, JsonNode> property : properties(body)) {
            if (!"status".equals(property.getKey())) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY, property.getKey(), "Property '" + property.getKey() + "' is not supported."));
            }
        }
        JsonNode status = body.get("status");
        if (status == null) {
            issues.add(FieldRules.required("status"));
            return null;
        }
        Optional<VocabularyStatus> parsed =
                status.isString() ? VocabularyStatus.parse(status.asString()) : Optional.empty();
        if (parsed.isEmpty()) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, "status", "'status' must be ACTIVE or DEPRECATED."));
            return null;
        }
        return parsed.get();
    }

    /**
     * Reads a JSON body; a body that cannot be read adds its issue instead of stopping the shape checks.
     *
     * @param request the request
     * @param maxBytes body limit (413 above it)
     * @param issues issue sink
     * @return the body, null when unreadable
     */
    static JsonNode readJson(jakarta.servlet.http.HttpServletRequest request, long maxBytes, List<Issue> issues) {
        try {
            return eu.simpl.vocabulary.api.request.JsonBody.read(request, maxBytes);
        } catch (eu.simpl.vocabulary.api.problem.ApiException e) {
            if (e.status() != 400) {
                throw e;
            }
            issues.addAll(e.issues());
            return null;
        }
    }

    /** Properties of a JSON object in order. */
    static List<Map.Entry<String, JsonNode>> properties(JsonNode object) {
        List<Map.Entry<String, JsonNode>> list = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = object.properties().iterator();
        iterator.forEachRemaining(list::add);
        return list;
    }

    /** A text form field, null when missing. */
    static String text(MultipartForm form, String name) {
        return form.first(name).map(MultipartForm.FormPart::text).orElse(null);
    }

    /** A Turtle download (D29, D70): exact Content-Type, attachment filename. */
    static ResponseEntity<byte[]> turtle(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TURTLE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /** A JSON response with explicit Content-Type. */
    static <T> ResponseEntity<T> json(int status, T body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}

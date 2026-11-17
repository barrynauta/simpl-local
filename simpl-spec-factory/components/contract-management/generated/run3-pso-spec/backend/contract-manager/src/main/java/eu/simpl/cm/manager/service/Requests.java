package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.common.Ids;

/** Body field checks (D84 step 5). */
public final class Requests {

    private Requests() {
    }

    /** A text field or null. Non-text values are a validation failure. */
    public static String text(JsonNode body, String field) {
        if (body == null) {
            return null;
        }
        JsonNode v = body.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (!v.isTextual()) {
            throw ApiException.validation("Field " + field + " must be a string");
        }
        return v.asText();
    }

    public static String requiredId(JsonNode body, String field) {
        String v = text(body, field);
        if (v == null || v.isBlank()) {
            throw ApiException.validation("Field " + field + " is required");
        }
        if (!Ids.isId(v)) {
            throw ApiException.validation("Field " + field + " has an invalid format");
        }
        return v;
    }

    public static String optionalId(JsonNode body, String field) {
        String v = text(body, field);
        if (v == null) {
            return null;
        }
        if (!Ids.isId(v)) {
            throw ApiException.validation("Field " + field + " has an invalid format");
        }
        return v;
    }

    public static String optionalUuid(JsonNode body, String field) {
        String v = text(body, field);
        if (v == null) {
            return null;
        }
        if (!Ids.isUuid(v)) {
            throw ApiException.validation("Field " + field + " must be a UUID");
        }
        return v;
    }

    public static void requireObject(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ApiException.validation("Request body must be a JSON object");
        }
    }
}

package eu.simpl.cm.common.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Manual JSON body handling so that 400 and 415 follow D11 exactly, also for optional bodies. */
public final class JsonBody {
    private JsonBody() {
    }

    public static boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        int semi = ct.indexOf(';');
        if (semi >= 0) {
            ct = ct.substring(0, semi);
        }
        ct = ct.trim();
        return ct.equals("application/json") || (ct.startsWith("application/") && ct.endsWith("+json"));
    }

    /** Returns null when there is no body and it is optional. */
    public static JsonNode read(ObjectMapper mapper, byte[] raw, String contentType, boolean required) {
        boolean empty = raw == null || new String(raw, java.nio.charset.StandardCharsets.UTF_8).isBlank();
        if (empty) {
            if (required) {
                throw ApiException.validation("Request body is required");
            }
            return null;
        }
        if (!isJson(contentType)) {
            throw new ApiException(415, Problems.UNSUPPORTED_MEDIA_TYPE, "Content type must be application/json");
        }
        try {
            JsonNode node = mapper.readTree(raw);
            if (node == null) {
                throw ApiException.validation("Invalid body content");
            }
            return node;
        } catch (java.io.IOException e) {
            throw ApiException.validation("Invalid body content");
        }
    }

    public static JsonNode readObject(ObjectMapper mapper, byte[] raw, String contentType, boolean required) {
        JsonNode n = read(mapper, raw, contentType, required);
        if (n != null && !n.isObject()) {
            throw ApiException.validation("A JSON object is expected");
        }
        return n;
    }

    /** Optional string field; non-string values are a validation failure. */
    public static String text(JsonNode body, String field) {
        if (body == null) {
            return null;
        }
        JsonNode v = body.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (!v.isTextual()) {
            throw ApiException.validation(field + " must be a string");
        }
        return v.asText();
    }

    public static String requiredId(JsonNode body, String field) {
        String v = text(body, field);
        if (v == null || v.isEmpty()) {
            throw ApiException.validation(field + " is required");
        }
        if (!Ids.isId(v)) {
            throw ApiException.validation(field + " has an invalid format");
        }
        return v;
    }

    public static String optionalId(JsonNode body, String field) {
        String v = text(body, field);
        if (v == null) {
            return null;
        }
        if (!Ids.isId(v)) {
            throw ApiException.validation(field + " has an invalid format");
        }
        return v;
    }
}

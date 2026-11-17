package eu.simpl.catalogue.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/** Shared Jackson helpers. */
public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);

    private Json() {}

    public static ObjectNode object() { return MAPPER.createObjectNode(); }
    public static ArrayNode array() { return MAPPER.createArrayNode(); }

    /** Parse JSON or return null when the text is not JSON. */
    public static JsonNode parseOrNull(String text) {
        if (text == null) return null;
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static JsonNode parse(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getOriginalMessage(), e);
        }
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Text of a literal: plain string, number, boolean, or the @value of a value object. */
    public static String literal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.isObject()) {
            JsonNode v = node.get("@value");
            if (v != null) return literal(v);
            JsonNode id = node.get("@id");
            if (id != null && id.isTextual()) return id.asText();
            return null;
        }
        if (node.isArray()) {
            return node.isEmpty() ? null : literal(node.get(0));
        }
        if (node.isNumber()) return node.asText();
        return node.asText();
    }

    public static List<String> textList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || node.isNull() || node.isMissingNode()) return out;
        if (node.isArray()) {
            node.forEach(n -> { String s = literal(n); if (s != null) out.add(s); });
        } else {
            String s = literal(node);
            if (s != null) out.add(s);
        }
        return out;
    }
}

package eu.simpl.cm.stubs.crypto;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON Canonicalization Scheme (RFC 8785) for the documents of this run: objects with keys
 * sorted by UTF-16 code unit, no whitespace, RFC 8785 string escaping. Numbers are written
 * with Jackson's shortest form, which matches ES6 for the integers used here.
 */
public final class Jcs {

    private Jcs() {
    }

    public static byte[] canonicalize(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void write(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            sb.append("null");
        } else if (node.isObject()) {
            List<String> keys = new ArrayList<>();
            Iterator<String> it = node.fieldNames();
            it.forEachRemaining(keys::add);
            keys.sort(String::compareTo); // String.compareTo compares UTF-16 code units, as RFC 8785 requires
            sb.append('{');
            boolean first = true;
            for (String k : keys) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                string(k, sb);
                sb.append(':');
                write(node.get(k), sb);
            }
            sb.append('}');
        } else if (node.isArray()) {
            sb.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                write(node.get(i), sb);
            }
            sb.append(']');
        } else if (node.isTextual()) {
            string(node.textValue(), sb);
        } else if (node.isBoolean()) {
            sb.append(node.booleanValue() ? "true" : "false");
        } else if (node.isIntegralNumber()) {
            sb.append(node.bigIntegerValue().toString());
        } else if (node.isNumber()) {
            double d = node.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e21) {
                sb.append(Long.toString((long) d));
            } else {
                sb.append(Double.toString(d));
            }
        } else {
            string(node.asText(), sb);
        }
    }

    private static void string(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    /** Helper for tests: canonical form of a map. */
    public static String toString(Map<String, ?> map, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        return new String(canonicalize(mapper.valueToTree(map)), StandardCharsets.UTF_8);
    }
}

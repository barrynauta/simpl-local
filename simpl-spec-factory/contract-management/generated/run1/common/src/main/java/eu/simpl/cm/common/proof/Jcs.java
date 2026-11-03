package eu.simpl.cm.common.proof;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * JSON Canonicalization Scheme (RFC 8785) for the documents used here.
 * Keys are sorted by UTF-16 code units, no whitespace, strings escaped as ECMAScript JSON.stringify does.
 * Numbers are written in a simple canonical form; the contract documents contain no numbers.
 */
public final class Jcs {
    private Jcs() {
    }

    public static byte[] canonicalBytes(JsonNode node) {
        return canonicalize(node).getBytes(StandardCharsets.UTF_8);
    }

    public static String canonicalize(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb);
        return sb.toString();
    }

    private static void write(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            sb.append("null");
        } else if (node.isBoolean()) {
            sb.append(node.booleanValue() ? "true" : "false");
        } else if (node.isNumber()) {
            sb.append(number(node));
        } else if (node.isTextual()) {
            writeString(node.textValue(), sb);
        } else if (node.isArray()) {
            sb.append('[');
            boolean first = true;
            for (JsonNode child : node) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(child, sb);
            }
            sb.append(']');
        } else if (node.isObject()) {
            List<String> keys = new ArrayList<>();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                keys.add(it.next());
            }
            keys.sort(String::compareTo);
            sb.append('{');
            boolean first = true;
            for (String key : keys) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(key, sb);
                sb.append(':');
                write(node.get(key), sb);
            }
            sb.append('}');
        } else {
            throw new IllegalArgumentException("Unsupported JSON node: " + node.getNodeType());
        }
    }

    private static String number(JsonNode node) {
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        double d = node.doubleValue();
        if (d == Math.rint(d) && Math.abs(d) < 1e21) {
            return new BigDecimal(d).toBigInteger().toString();
        }
        return Double.toString(d);
    }

    static void writeString(String s, StringBuilder sb) {
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
}

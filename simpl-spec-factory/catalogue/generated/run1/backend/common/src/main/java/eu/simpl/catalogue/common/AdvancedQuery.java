package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Advanced search body of D29. Root keys select nodes by their rdf:type, inner keys are property local names.
 * String values match as case-insensitive substrings, numbers and range objects compare numerically.
 * All criteria are combined with AND.
 */
public final class AdvancedQuery {
    public static final Set<String> RESERVED = Set.of("catalogueIds", "minimumScore", "minimumClassification");
    private static final Set<String> RANGE_KEYS = Set.of("op1", "op2", "value1", "value2");
    private static final Set<String> OPERATORS = Set.of("=", ">", ">=", "<", "<=");
    private static final String SIMPL = "http://w3id.org/gaia-x/simpl#";

    public sealed interface Criterion permits Text, Range {}
    public record Text(String value) implements Criterion {}
    public record Range(String op1, BigDecimal value1, String op2, BigDecimal value2) implements Criterion {}
    public record NodeCriteria(String nodeType, Map<String, Criterion> properties) {}

    private final List<NodeCriteria> nodes;

    private AdvancedQuery(List<NodeCriteria> nodes) { this.nodes = nodes; }

    public List<NodeCriteria> nodes() { return nodes; }

    /** Parse the body text; any structural problem is a 400. */
    public static AdvancedQuery parse(String body) {
        JsonNode root = Json.parseOrNull(body);
        if (root == null || !root.isObject()) {
            throw ApiException.badRequest("The request body must be a JSON object.");
        }
        return parse(root);
    }

    public static AdvancedQuery parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw ApiException.badRequest("The request body must be a JSON object.");
        }
        List<NodeCriteria> out = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            if (RESERVED.contains(key) || key.startsWith("@")) continue;
            JsonNode value = e.getValue();
            if (!value.isObject()) {
                throw ApiException.badRequest("Node " + key + " must be a JSON object of property names and searched values.");
            }
            Map<String, Criterion> props = new java.util.LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> pit = value.fields();
            while (pit.hasNext()) {
                Map.Entry<String, JsonNode> p = pit.next();
                if (p.getKey().startsWith("@")) continue;
                props.put(p.getKey(), criterion(key, p.getKey(), p.getValue()));
            }
            if (props.isEmpty()) {
                throw ApiException.badRequest("No Properties defined in " + key
                        + " node. Please provide at least one property with searched value.");
            }
            out.add(new NodeCriteria(key, props));
        }
        return new AdvancedQuery(out);
    }

    private static Criterion criterion(String node, String prop, JsonNode v) {
        if (v.isTextual()) return new Text(v.asText());
        if (v.isNumber()) return new Range("=", v.decimalValue(), null, null);
        if (v.isBoolean()) return new Text(v.asText());
        if (v.isObject()) {
            Iterator<String> names = v.fieldNames();
            while (names.hasNext()) {
                if (!RANGE_KEYS.contains(names.next())) {
                    throw ApiException.badRequest("Wrong property names in node " + node
                            + ". Allowed property names: op1, op2, value1, value2");
                }
            }
            if (!v.hasNonNull("op1") || !v.hasNonNull("value1")) {
                throw ApiException.badRequest("Range for property " + prop + " in node " + node
                        + " requires op1 and value1.");
            }
            String op1 = op(v.get("op1"), node);
            BigDecimal v1 = number(v.get("value1"), node);
            String op2 = null;
            BigDecimal v2 = null;
            if (v.hasNonNull("op2") || v.hasNonNull("value2")) {
                if (!v.hasNonNull("op2") || !v.hasNonNull("value2")) {
                    throw ApiException.badRequest("Range for property " + prop + " in node " + node
                            + " needs both op2 and value2.");
                }
                op2 = op(v.get("op2"), node);
                v2 = number(v.get("value2"), node);
            }
            return new Range(op1, v1, op2, v2);
        }
        throw ApiException.badRequest("Unsupported value for property " + prop + " in node " + node + ".");
    }

    private static String op(JsonNode n, String node) {
        String s = n.asText();
        if (!n.isTextual() || !OPERATORS.contains(s)) {
            throw ApiException.badRequest("Unknown operator '" + s + "' in node " + node + ". Allowed operators: =, >, >=, <, <=");
        }
        return s;
    }

    private static BigDecimal number(JsonNode n, String node) {
        try {
            if (n.isNumber()) return n.decimalValue();
            if (n.isTextual()) return new BigDecimal(n.asText().trim());
        } catch (NumberFormatException ignored) {
            // fall through
        }
        throw ApiException.badRequest("Range values in node " + node + " must be numeric.");
    }

    /** Copy of the body without the reserved root fields. */
    public static ObjectNode withoutReserved(JsonNode body, boolean keepQuality) {
        ObjectNode copy = ((ObjectNode) body).deepCopy();
        copy.remove("catalogueIds");
        if (!keepQuality) {
            copy.remove("minimumScore");
            copy.remove("minimumClassification");
        }
        return copy;
    }

    // ---------------------------------------------------------------- matching

    public boolean matches(JsonNode vc) {
        JsonNode subject = vc.path("credentialSubject");
        for (NodeCriteria nc : nodes) {
            List<JsonNode> candidates = new ArrayList<>();
            collectNodes(subject, compact(nc.nodeType()), candidates);
            boolean any = false;
            for (JsonNode n : candidates) {
                if (nodeMatches(n, nc.properties())) { any = true; break; }
            }
            if (!any) return false;
        }
        return true;
    }

    private static boolean nodeMatches(JsonNode node, Map<String, Criterion> props) {
        for (Map.Entry<String, Criterion> p : props.entrySet()) {
            List<JsonNode> values = propertyValues(node, p.getKey());
            boolean any = false;
            for (JsonNode v : values) {
                if (valueMatches(v, p.getValue())) { any = true; break; }
            }
            if (!any) return false;
        }
        return true;
    }

    private static boolean valueMatches(JsonNode v, Criterion c) {
        String lit = Json.literal(v);
        if (lit == null) return false;
        if (c instanceof Text t) {
            return lit.toLowerCase(Locale.ROOT).contains(t.value().toLowerCase(Locale.ROOT));
        }
        Range r = (Range) c;
        BigDecimal d;
        try {
            d = new BigDecimal(lit.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return compare(d, r.op1(), r.value1()) && (r.op2() == null || compare(d, r.op2(), r.value2()));
    }

    private static boolean compare(BigDecimal d, String op, BigDecimal ref) {
        int c = d.compareTo(ref);
        return switch (op) {
            case "=" -> c == 0;
            case ">" -> c > 0;
            case ">=" -> c >= 0;
            case "<" -> c < 0;
            case "<=" -> c <= 0;
            default -> false;
        };
    }

    private static List<JsonNode> propertyValues(JsonNode node, String localName) {
        List<JsonNode> out = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getKey().startsWith("@")) continue;
            if (localName(e.getKey()).equals(localName)) {
                if (e.getValue().isArray()) e.getValue().forEach(out::add);
                else out.add(e.getValue());
            }
        }
        return out;
    }

    private static void collectNodes(JsonNode n, String type, List<JsonNode> out) {
        if (n.isArray()) {
            n.forEach(x -> collectNodes(x, type, out));
            return;
        }
        if (!n.isObject()) return;
        if (types(n).contains(type)) out.add(n);
        Iterator<Map.Entry<String, JsonNode>> it = n.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getKey().equals("@context")) continue;
            if (e.getValue().isContainerNode()) collectNodes(e.getValue(), type, out);
        }
    }

    private static List<String> types(JsonNode n) {
        List<String> out = new ArrayList<>();
        for (String k : List.of("rdf:type", "@type", "type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
            JsonNode t = n.get(k);
            if (t == null) continue;
            for (String s : Json.textList(t)) out.add(compact(s));
        }
        return out;
    }

    public static String compact(String iri) {
        if (iri == null) return null;
        if (iri.startsWith(SIMPL)) return "simpl:" + iri.substring(SIMPL.length());
        return iri;
    }

    public static String localName(String key) {
        int i = Math.max(Math.max(key.lastIndexOf(':'), key.lastIndexOf('#')), key.lastIndexOf('/'));
        return i < 0 ? key : key.substring(i + 1);
    }
}

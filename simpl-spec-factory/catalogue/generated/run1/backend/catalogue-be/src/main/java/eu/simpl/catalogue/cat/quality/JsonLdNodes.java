package eu.simpl.catalogue.cat.quality;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.Json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lenient reading of the compact JSON-LD used by the QAR and the quality profile. */
final class JsonLdNodes {
    private JsonLdNodes() {}

    /** Every object with an @id, found anywhere in the document, keyed by @id (first wins). */
    static Map<String, JsonNode> index(JsonNode doc) {
        Map<String, JsonNode> out = new LinkedHashMap<>();
        collect(doc, out);
        return out;
    }

    private static void collect(JsonNode n, Map<String, JsonNode> out) {
        if (n == null) return;
        if (n.isArray()) {
            n.forEach(x -> collect(x, out));
        } else if (n.isObject()) {
            if (n.path("@id").isTextual() && n.size() > 1) out.putIfAbsent(n.get("@id").asText(), n);
            Iterator<Map.Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (!e.getKey().equals("@context")) collect(e.getValue(), out);
            }
        }
    }

    static boolean isType(JsonNode node, String localType) {
        for (String t : Json.textList(node.get("@type"))) {
            if (local(t).equals(localType)) return true;
        }
        return false;
    }

    static List<JsonNode> ofType(Map<String, JsonNode> index, String localType) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode n : index.values()) if (isType(n, localType)) out.add(n);
        return out;
    }

    /** Property by local name, ignoring any prefix. */
    static JsonNode prop(JsonNode node, String localName) {
        if (node == null) return null;
        JsonNode direct = node.get(localName);
        if (direct != null) return direct;
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (local(e.getKey()).equals(localName)) return e.getValue();
        }
        return null;
    }

    static Double number(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.asDouble();
        if (n.isObject() && n.has("@value")) return number(n.get("@value"));
        if (n.isTextual()) {
            try {
                return Double.parseDouble(n.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** Reference values: "id", {"@id": "id"} or embedded objects with @id; single or array. */
    static List<String> refs(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n == null || n.isNull()) return out;
        if (n.isArray()) {
            n.forEach(x -> out.addAll(refs(x)));
        } else if (n.isTextual()) {
            out.add(n.asText());
        } else if (n.isObject() && n.path("@id").isTextual()) {
            out.add(n.get("@id").asText());
        }
        return out;
    }

    static String ref(JsonNode n) {
        List<String> r = refs(n);
        return r.isEmpty() ? null : r.get(0);
    }

    static String local(String iri) {
        int i = Math.max(Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/')), iri.lastIndexOf(':'));
        return i < 0 ? iri : iri.substring(i + 1);
    }
}

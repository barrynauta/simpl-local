package eu.simpl.catalogue.cat.quality;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A parsed Quality Assessment Record with the structure checks of D55. */
public final class Qar {
    public record DimensionScore(String dimension, double score) {}

    public final JsonNode doc;
    public final Map<String, JsonNode> index;
    public final double overall;
    public final Map<String, Double> dimensionScores = new LinkedHashMap<>();
    public final List<JsonNode> ruleEvaluations;

    private Qar(JsonNode doc, Map<String, JsonNode> index, double overall, List<JsonNode> rules) {
        this.doc = doc;
        this.index = index;
        this.overall = overall;
        this.ruleEvaluations = rules;
    }

    public static class InvalidQarException extends RuntimeException {
        public InvalidQarException(String m) { super(m); }
    }

    public static Qar parse(JsonNode doc, String rdId) {
        if (doc == null || !doc.isObject()) throw new InvalidQarException("The QAR is not a JSON object.");
        JsonNode graph = doc.get("@graph");
        if (graph == null || !graph.isArray() || graph.isEmpty()) {
            throw new InvalidQarException("The Quality Assessment Record must contain a non-empty graph.");
        }
        for (JsonNode n : graph) {
            if (!n.isObject() || !n.path("@id").isTextual() || n.get("@type") == null) {
                throw new InvalidQarException("The Quality Assessment Record must contain a non-empty graph with an id and type for every item.");
            }
        }
        Map<String, JsonNode> index = new LinkedHashMap<>();
        graph.forEach(n -> index.putIfAbsent(n.get("@id").asText(), n));
        List<JsonNode> qars = filterTop(graph, "QualityAssessmentRecord");
        if (qars.size() != 1) throw new InvalidQarException("The graph must contain exactly one QualityAssessmentRecord node.");
        String computedOn = JsonLdNodes.ref(JsonLdNodes.prop(qars.get(0), "computedOn"));
        if (rdId != null && !rdId.equals(computedOn)) {
            throw new InvalidQarException("The QualityAssessmentRecord computedOn must equal the resource description id.");
        }
        List<JsonNode> sds = filterTop(graph, "SelfDescriptionScore");
        if (sds.size() != 1) throw new InvalidQarException("The graph must contain exactly one SelfDescriptionScore node.");
        Double score = JsonLdNodes.number(JsonLdNodes.prop(sds.get(0), "score"));
        Double max = JsonLdNodes.number(JsonLdNodes.prop(sds.get(0), "maxScore"));
        if (score == null || max == null || max <= 0) {
            throw new InvalidQarException("The SelfDescriptionScore must have a numeric score and a maxScore greater than 0.");
        }
        Qar q = new Qar(doc, index, 100.0 * score / max, filterTop(graph, "RuleEvaluation"));
        for (JsonNode d : filterTop(graph, "DimensionScore")) {
            String dim = JsonLdNodes.ref(JsonLdNodes.prop(d, "dimension"));
            Double s = JsonLdNodes.number(JsonLdNodes.prop(d, "score"));
            Double m = JsonLdNodes.number(JsonLdNodes.prop(d, "maxScore"));
            if (dim == null || s == null) continue;
            double mm = m == null || m <= 0 ? 100.0 : m;
            q.dimensionScores.putIfAbsent(dim, 100.0 * s / mm);
        }
        return q;
    }

    private static List<JsonNode> filterTop(JsonNode graph, String type) {
        List<JsonNode> out = new java.util.ArrayList<>();
        graph.forEach(n -> { if (JsonLdNodes.isType(n, type)) out.add(n); });
        return out;
    }
}

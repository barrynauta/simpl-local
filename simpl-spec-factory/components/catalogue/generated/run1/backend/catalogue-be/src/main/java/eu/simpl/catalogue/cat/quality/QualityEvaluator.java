package eu.simpl.catalogue.cat.quality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.Json;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Threshold evaluation (D51) and the SelfDescriptionQualityAssessment summary. */
public final class QualityEvaluator {
    private QualityEvaluator() {}

    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String m) { super(m); }
    }

    /** Returns the rejection message of the first failed threshold, or empty when all pass. */
    public static Optional<String> firstFailure(Qar qar, QualityProfile profile) {
        if (profile.thresholds.isEmpty()) {
            throw new ConfigurationException("The active Metadata Quality Profile defines no applicable threshold.");
        }
        for (QualityProfile.Threshold t : profile.thresholds) {
            if (t.scope() == null && qar.overall < t.value()) {
                return Optional.of(message(qar.overall, t.value()));
            }
        }
        for (String dim : orderedDimensions(qar, profile)) {
            for (QualityProfile.Threshold t : profile.thresholds) {
                if (dim.equals(t.scope())) {
                    Double s = qar.dimensionScores.get(dim);
                    if (s == null) throw new Qar.InvalidQarException("The QAR has no score for dimension " + dim + ".");
                    if (s < t.value()) return Optional.of(message(s, t.value()));
                }
            }
        }
        return Optional.empty();
    }

    public static String message(double score, double threshold) {
        return "Quality Score " + fmt(score) + " is below treshold " + fmt(threshold) + ".";
    }

    public static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static List<String> orderedDimensions(Qar qar, QualityProfile profile) {
        Set<String> dims = new LinkedHashSet<>(profile.dimensions);
        for (QualityProfile.Threshold t : profile.thresholds) if (t.scope() != null) dims.add(t.scope());
        dims.addAll(qar.dimensionScores.keySet());
        return new ArrayList<>(dims);
    }

    /** The summary stored with the RD and returned at publication (schema SelfDescriptionQualityAssessment). */
    public static ObjectNode summary(Qar qar, QualityProfile profile) {
        ObjectNode out = Json.object();
        ArrayNode rules = out.putArray("rules");
        for (JsonNode r : qar.ruleEvaluations) {
            ObjectNode rn = rules.addObject();
            String id = r.path("@id").asText();
            rn.put("id", id);
            JsonNode name = JsonLdNodes.prop(r, "name");
            rn.put("name", name != null && !name.isNull() ? name.asText() : JsonLdNodes.local(id));
            JsonNode result = JsonLdNodes.prop(r, "result");
            boolean passed = result != null && result.asText().equalsIgnoreCase("passed");
            rn.put("status", passed ? "PASSED" : "FAILED");
        }
        ArrayNode dims = out.putArray("dimensions");
        for (String dim : orderedDimensions(qar, profile)) {
            Double s = qar.dimensionScores.get(dim);
            if (s == null) continue;
            ObjectNode dn = dims.addObject();
            dn.put("name", profile.dimensionName(dim, qar.index));
            dn.put("score", s);
            dn.put("classification", profile.classify(s));
            for (QualityProfile.Threshold t : profile.thresholds) {
                if (dim.equals(t.scope())) dn.put("thresholdStatus", s >= t.value() ? "PASSED" : "FAILED");
            }
        }
        ObjectNode overall = out.putObject("overall");
        overall.put("score", qar.overall);
        overall.put("classification", profile.classify(qar.overall));
        String status = "PASSED";
        for (QualityProfile.Threshold t : profile.thresholds) {
            if (t.scope() == null && qar.overall < t.value()) status = "FAILED";
        }
        overall.put("thresholdStatus", status);
        return out;
    }
}

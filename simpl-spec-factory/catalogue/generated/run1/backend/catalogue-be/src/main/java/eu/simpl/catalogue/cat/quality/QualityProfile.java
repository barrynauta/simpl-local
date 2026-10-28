package eu.simpl.catalogue.cat.quality;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Metadata Quality Profile as served by the quality stub (D50, D51, D53). */
public final class QualityProfile {
    public record Range(String label, double min, double max) {}
    /** scope is null for "overall", else the dimension IRI. */
    public record Threshold(String id, String scope, double value) {}

    public final String id;
    public final String version;
    public final List<String> dimensions = new ArrayList<>();
    public final List<Threshold> thresholds = new ArrayList<>();
    public final List<Range> ranges = new ArrayList<>();
    public final Map<String, JsonNode> index;

    private QualityProfile(String id, String version, Map<String, JsonNode> index) {
        this.id = id;
        this.version = version;
        this.index = index;
    }

    public static QualityProfile parse(JsonNode doc) {
        Map<String, JsonNode> index = JsonLdNodes.index(doc);
        JsonNode profile = null;
        for (JsonNode n : index.values()) {
            if (JsonLdNodes.isType(n, "MetadataQualityProfile")) { profile = n; break; }
        }
        if (profile == null && doc.isObject()) profile = doc;
        QualityProfile p = new QualityProfile(
                profile.path("@id").asText(null),
                textOf(JsonLdNodes.prop(profile, "profileVersion")), index);
        p.dimensions.addAll(JsonLdNodes.refs(JsonLdNodes.prop(profile, "includesDimension")));
        for (String tid : JsonLdNodes.refs(JsonLdNodes.prop(profile, "includesThreshold"))) {
            JsonNode t = index.get(tid);
            if (t == null) continue;
            Double v = null;
            for (String k : List.of("thresholdValue", "value", "threshold", "minScore", "score")) {
                v = JsonLdNodes.number(JsonLdNodes.prop(t, k));
                if (v != null) break;
            }
            if (v == null) continue;
            JsonNode scope = JsonLdNodes.prop(t, "classificationScope");
            String s = scope == null ? "overall" : JsonLdNodes.ref(scope);
            p.thresholds.add(new Threshold(tid, s == null || s.equalsIgnoreCase("overall")
                    || JsonLdNodes.local(s).equalsIgnoreCase("overall") ? null : s, v));
        }
        for (JsonNode r : JsonLdNodes.ofType(index, "ScoreRange")) {
            Double min = JsonLdNodes.number(JsonLdNodes.prop(r, "minScore"));
            Double max = JsonLdNodes.number(JsonLdNodes.prop(r, "maxScore"));
            String cls = JsonLdNodes.ref(JsonLdNodes.prop(r, "rangeClassification"));
            if (min == null || max == null || cls == null) continue;
            p.ranges.add(new Range(JsonLdNodes.local(cls), min, max));
        }
        p.ranges.sort(Comparator.comparingDouble(Range::min));
        return p;
    }

    private static String textOf(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isObject() && n.has("@value")) return n.get("@value").asText();
        return n.asText();
    }

    /** Label of the range that contains the score (min <= s < max; the top range includes its max). */
    public String classify(Double score) {
        if (score == null || ranges.isEmpty()) return null;
        Range top = ranges.stream().max(Comparator.comparingDouble(Range::max)).orElseThrow();
        for (Range r : ranges) {
            if (score >= r.min() && (score < r.max() || (r == top && score <= r.max()))) return r.label();
        }
        return null;
    }

    /** Rank of a label ordered by the lower bound of its range, or -1 if unknown (case-insensitive). */
    public int rank(String label) {
        if (label == null) return -1;
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i).label().toLowerCase(Locale.ROOT).equals(label.toLowerCase(Locale.ROOT))) return i;
        }
        return -1;
    }

    public List<String> labels() {
        return ranges.stream().map(Range::label).distinct().toList();
    }

    public String dimensionName(String dimensionId, Map<String, JsonNode> qarIndex) {
        JsonNode d = qarIndex.get(dimensionId);
        if (d == null) d = index.get(dimensionId);
        String name = d == null ? null : textOf(JsonLdNodes.prop(d, "name"));
        if (name != null) return name;
        String l = JsonLdNodes.local(dimensionId);
        return l.isEmpty() ? dimensionId : Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }
}

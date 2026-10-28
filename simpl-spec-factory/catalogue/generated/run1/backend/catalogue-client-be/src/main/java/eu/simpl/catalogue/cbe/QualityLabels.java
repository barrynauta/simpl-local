package eu.simpl.catalogue.cbe;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.QualityParams;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Classification labels of the default profile, to validate minimumClassification (D53, D54). */
@Component
public class QualityLabels {
    private final Downstream down;
    private final CbeProps props;
    private List<String> labels;
    private Instant expires = Instant.EPOCH;

    public QualityLabels(Downstream down, CbeProps props) {
        this.down = down;
        this.props = props;
    }

    public synchronized List<String> labels() {
        if (labels != null && Instant.now().isBefore(expires)) return labels;
        Downstream.Res r = down.call(HttpMethod.GET, URI.create(props.getQualityStubUrl() + "/v1/profiles/active"),
                null, null, "application/json");
        if (!r.ok()) throw new ApiException(ProblemTypes.INTERNAL, "The quality profile could not be read.");
        labels = parse(Json.parseOrNull(r.body()));
        expires = Instant.now().plusSeconds(30);
        return labels;
    }

    public void validate(QualityParams q) {
        if (q.minimumClassification() == null) return;
        String wanted = q.minimumClassification().toLowerCase(Locale.ROOT);
        if (labels().stream().noneMatch(l -> l.toLowerCase(Locale.ROOT).equals(wanted))) {
            throw ApiException.invalidInput(QualityParams.CLASSIFICATION_DETAIL);
        }
    }

    static List<String> parse(JsonNode doc) {
        List<String> out = new ArrayList<>();
        collect(doc, out);
        return out;
    }

    private static void collect(JsonNode n, List<String> out) {
        if (n == null) return;
        if (n.isArray()) {
            n.forEach(x -> collect(x, out));
            return;
        }
        if (!n.isObject()) return;
        boolean range = Json.textList(n.get("@type")).stream().anyMatch(t -> t.endsWith("ScoreRange"));
        if (range) {
            JsonNode c = n.get("rangeClassification");
            String iri = c == null ? null : c.isTextual() ? c.asText() : c.path("@id").asText(null);
            if (iri != null) {
                int i = Math.max(Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/')), iri.lastIndexOf(':'));
                String label = iri.substring(i + 1);
                if (!out.contains(label)) out.add(label);
            }
        }
        n.fields().forEachRemaining(e -> { if (!e.getKey().equals("@context")) collect(e.getValue(), out); });
    }
}

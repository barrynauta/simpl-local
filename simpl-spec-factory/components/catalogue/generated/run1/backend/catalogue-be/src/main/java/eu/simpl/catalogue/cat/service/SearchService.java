package eu.simpl.catalogue.cat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.quality.QualityClient;
import eu.simpl.catalogue.cat.quality.QualityProfile;
import eu.simpl.catalogue.cat.store.RdRecord;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.AccessPolicy;
import eu.simpl.catalogue.common.AdvancedQuery;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.common.RdProjection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Local quick and advanced search with discovery-policy and quality filtering (D27 to D31, D54). */
@Service
public class SearchService {
    private static final Set<String> SKIP_KEYS = Set.of("@context", "@type", "rdf:type", "simpl:servicePolicy");

    private final RdRepository repo;
    private final QualityClient quality;

    public SearchService(RdRepository repo, QualityClient quality) {
        this.repo = repo;
        this.quality = quality;
    }

    public ObjectNode quick(String searchTerm, Collection<String> attributes, QualityParams q, Paging paging) {
        List<String> terms = new ArrayList<>();
        for (String t : searchTerm.trim().split("\\s+")) if (!t.isEmpty()) terms.add(t.toLowerCase(Locale.ROOT));
        return run(vc -> matchesAnyTerm(vc.path("credentialSubject"), terms), attributes, q, paging);
    }

    public ObjectNode advanced(AdvancedQuery query, Collection<String> attributes, QualityParams q, Paging paging) {
        return run(query::matches, attributes, q, paging);
    }

    private ObjectNode run(Predicate<JsonNode> match, Collection<String> attributes, QualityParams q, Paging paging) {
        Predicate<RdRecord> qualityFilter = qualityFilter(q);
        List<ObjectNode> hits = new ArrayList<>();
        for (RdRecord r : repo.active()) {
            JsonNode vc = r.vc();
            if (!AccessPolicy.discoverable(vc, attributes)) continue;
            if (!match.test(vc)) continue;
            if (!qualityFilter.test(r)) continue;
            hits.add(item(r, vc));
        }
        ObjectNode out = Json.object();
        out.put("totalCount", hits.size());
        ArrayNode items = out.putArray("items");
        hits.stream().skip(paging.offset()).limit(paging.pageSize()).forEach(items::add);
        return out;
    }

    private static ObjectNode item(RdRecord r, JsonNode vc) {
        ObjectNode n = RdProjection.mainProperties(vc);
        if (r.overallScore() == null) n.putNull("overallScore"); else n.put("overallScore", r.overallScore());
        n.put("overallClassification", r.overallClass());
        ObjectNode wrapper = Json.object();
        wrapper.set("n", n);
        return wrapper;
    }

    /** Validates the classification against the default profile and builds the AND filter of D54. */
    public Predicate<RdRecord> qualityFilter(QualityParams q) {
        if (q == null || !q.isSet()) return r -> true;
        Integer minRank = null;
        QualityProfile profile = null;
        if (q.minimumClassification() != null) {
            profile = quality.defaultProfile();
            int rank = profile.rank(q.minimumClassification());
            if (rank < 0) throw ApiException.invalidInput(QualityParams.CLASSIFICATION_DETAIL);
            minRank = rank;
        }
        final Integer fr = minRank;
        final QualityProfile fp = profile;
        return r -> {
            if (q.minimumScore() != null && (r.overallScore() == null || r.overallScore() < q.minimumScore())) return false;
            if (fr != null) {
                int rank = fp.rank(r.overallClass());
                return rank >= 0 && rank >= fr;
            }
            return true;
        };
    }

    public void validateClassification(QualityParams q) {
        qualityFilter(q);
    }

    static boolean matchesAnyTerm(JsonNode subject, List<String> terms) {
        if (terms.isEmpty()) return false;
        List<String> values = new ArrayList<>();
        collect(subject, values);
        for (String v : values) {
            String lv = v.toLowerCase(Locale.ROOT);
            for (String t : terms) if (lv.contains(t)) return true;
        }
        return false;
    }

    private static void collect(JsonNode n, List<String> out) {
        if (n.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (SKIP_KEYS.contains(e.getKey())) continue;
                collect(e.getValue(), out);
            }
        } else if (n.isArray()) {
            n.forEach(x -> collect(x, out));
        } else if (n.isTextual() || n.isNumber() || n.isBoolean()) {
            out.add(n.asText());
        }
    }
}

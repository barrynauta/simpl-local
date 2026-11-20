package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.Rdf;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

/** The two read-only searches of D38. Neither is paged; every reference matches at most one result. */
public class SearchService {

    private final VocabularyRepository vocabularies;
    private final ExternalVocabularyRepository externals;

    public SearchService(VocabularyRepository vocabularies, ExternalVocabularyRepository externals) {
        this.vocabularies = vocabularies;
        this.externals = externals;
    }

    /** One reference of an internal search: a name, with a version or without (latest). */
    public record Reference(String name, Integer version) {}

    /** One internal search result (VocabularySearchItem). */
    public record Hit(String name, int version, VocabularyStatus status, boolean latest) {}

    /**
     * Resolves the references in request order; repeated references give one result at their first position and
     * missing names or versions are left out (D38).
     */
    public List<Hit> searchInternal(List<Reference> references) {
        List<Reference> unique = new ArrayList<>(new LinkedHashSet<>(references));
        Set<String> latestNames = new LinkedHashSet<>();
        List<List<? extends RDFNode>> pairs = new ArrayList<>();
        for (Reference reference : unique) {
            if (reference.version() == null) {
                latestNames.add(reference.name());
            } else {
                pairs.add(List.of(Rdf.text(reference.name()), Rdf.integer(reference.version())));
            }
        }
        Map<String, Hit> latest = new HashMap<>();
        if (!latestNames.isEmpty()) {
            for (QuerySolution row : vocabularies.searchLatest(latestNames)) {
                String name = Rdf.string(row, "name");
                latest.put(
                        name,
                        new Hit(
                                name,
                                Rdf.integer(row, "latest"),
                                VocabularyStatus.valueOf(Rdf.string(row, "status")),
                                true));
            }
        }
        Map<Reference, Hit> versioned = new HashMap<>();
        if (!pairs.isEmpty()) {
            for (QuerySolution row : vocabularies.searchVersions(pairs)) {
                String name = Rdf.string(row, "name");
                int version = Rdf.integer(row, "version");
                versioned.put(
                        new Reference(name, version),
                        new Hit(name, version, VocabularyStatus.valueOf(Rdf.string(row, "status")), false));
            }
        }
        List<Hit> hits = new ArrayList<>();
        for (Reference reference : unique) {
            Hit hit = reference.version() == null ? latest.get(reference.name()) : versioned.get(reference);
            if (hit != null) {
                hits.add(hit);
            }
        }
        return hits;
    }

    /**
     * Looks the URIs up (normalised, D30) against namespace and alias keys; results in the order of the first
     * matching URI, each external vocabulary once (D38).
     */
    public List<ExternalVocabulary> searchExternal(List<String> uris) {
        List<String> keys =
                uris.stream().map(ExternalVocabulary::key).distinct().toList();
        Map<String, String> namesByKey = externals.namesByKey(keys);
        Set<String> orderedNames = new LinkedHashSet<>();
        for (String key : keys) {
            String name = namesByKey.get(key);
            if (name != null) {
                orderedNames.add(name);
            }
        }
        Map<String, ExternalVocabulary> byName = new LinkedHashMap<>();
        externals.findAll(orderedNames).forEach(external -> byName.put(external.name(), external));
        return orderedNames.stream()
                .map(byName::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}

package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.InternalFacts;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.ReferenceFacts;
import eu.simpl.vocabulary.store.VocabularyStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The read-only search operations of D38: one store query each, results in request order. */
public class SearchService {

    /**
     * One reference of an internal search.
     *
     * @param name exact name (D54)
     * @param version version, null for the latest version
     */
    public record SearchReference(String name, Long version) {}

    /**
     * One internal search result.
     *
     * @param name name
     * @param version version number
     * @param status vocabulary status when latest, else version status
     * @param latest true for a reference without version
     */
    public record SearchResult(String name, long version, VocabularyStatus status, boolean latest) {}

    private final VocabularyStore store;

    public SearchService(VocabularyStore store) {
        this.store = store;
    }

    /**
     * Resolves internal references; missing ones are left out, repeated ones give one result.
     *
     * @param references the references in request order
     * @return the results
     */
    public List<SearchResult> searchInternal(List<SearchReference> references) {
        Set<SearchReference> distinct = new LinkedHashSet<>(references);
        Set<String> names = new LinkedHashSet<>();
        distinct.forEach(r -> names.add(r.name()));
        ReferenceFacts facts = store.referenceFacts(names, false);
        List<SearchResult> results = new ArrayList<>();
        for (SearchReference reference : distinct) {
            InternalFacts vocabulary = facts.vocabularies().get(reference.name());
            if (vocabulary == null || vocabulary.versions().isEmpty()) {
                continue;
            }
            if (reference.version() == null) {
                results.add(new SearchResult(
                        vocabulary.name(), vocabulary.versions().lastKey(), vocabulary.status(), true));
            } else {
                VocabularyStatus status = vocabulary.versions().get(reference.version());
                if (status != null) {
                    results.add(new SearchResult(vocabulary.name(), reference.version(), status, false));
                }
            }
        }
        return results;
    }

    /**
     * Finds external vocabularies by namespace or alias URI, normalised (D30, D38).
     *
     * @param uris URIs in request order, namespaceUris before aliasUris
     * @return each matching external vocabulary once, in order of the first matching URI
     */
    public List<ExternalVocabulary> searchExternal(List<String> uris) {
        Map<String, ExternalVocabulary> byKey = new LinkedHashMap<>();
        for (ExternalVocabulary external : store.listExternal()) {
            byKey.put(FieldRules.normaliseNamespace(external.namespaceUri()), external);
            external.aliasUris().forEach(alias -> byKey.put(FieldRules.normaliseNamespace(alias), external));
        }
        Map<String, ExternalVocabulary> results = new LinkedHashMap<>();
        for (String uri : uris) {
            ExternalVocabulary match = byKey.get(FieldRules.normaliseNamespace(uri));
            if (match != null) {
                results.putIfAbsent(match.name(), match);
            }
        }
        return List.copyOf(results.values());
    }
}

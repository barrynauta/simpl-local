package eu.simpl.vocabularymanager.domain;

import eu.simpl.vocabularymanager.domain.Model.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Model.Page;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.store.ExternalStore;
import eu.simpl.vocabularymanager.store.ExternalStore.NewExternal;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.References;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * External vocabularies: register, read, search, delete (D30, D38, D41, D42, D56, D66, D73, D147,
 * D148).
 */
@Service
public class ExternalService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalService.class);

    /**
     * Content of an external vocabulary with its name.
     *
     * @param name name, for the filename
     * @param bytes content as registered
     */
    public record NamedContent(String name, byte[] bytes) {}

    /**
     * A registration request after the shape checks.
     *
     * @param name name
     * @param namespaceUri as sent
     * @param aliasUris as sent
     * @param sourceUri optional
     * @param description optional, trimmed, null when blank
     * @param bytes file content
     */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] bytes) {}

    private final ExternalStore store;
    private final ContentValidator validator;

    public ExternalService(ExternalStore store, ContentValidator validator) {
        this.store = store;
        this.validator = validator;
    }

    public static ApiException notFound(String name) {
        String d = "External vocabulary with name '" + name + "' was not found.";
        return ApiException.notFound(d, Issue.notFound("path", "name", d));
    }

    public ExternalVocabulary get(String name) {
        return store.find(name).orElseThrow(() -> notFound(name));
    }

    public Page<ExternalVocabulary> list(int page, int pageSize) {
        return store.list(page, pageSize);
    }

    public byte[] content(String name) {
        get(name);
        return store.content(name).orElseThrow(() -> notFound(name));
    }

    /** POST /external-vocabularies/content: lookup by namespace or alias (D30, D66). */
    public NamedContent contentByNamespace(String namespaceUri) {
        String key = References.normalise(namespaceUri);
        String owner = store.ownersOfKeys(List.of(key)).get(key);
        if (owner != null) {
            var bytes = store.content(owner);
            if (bytes.isPresent()) {
                return new NamedContent(owner, bytes.get());
            }
        }
        String d = "External vocabulary with namespaceUri '" + namespaceUri + "' was not found.";
        throw ApiException.notFound(d, Issue.notFound("body", "namespaceUri", d));
    }

    public void delete(String name) {
        get(name);
        store.delete(name);
        LOG.info("external vocabulary removed");
    }

    /** Registration after the shape checks: 409 in the order of D147, syntax check, write. */
    public ExternalVocabulary register(Registration r, String creator) {
        conflictCheck(r);
        validator.validateExternal(r.bytes(), r.namespaceUri());
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        List<String> aliasKeys = new ArrayList<>();
        for (String a : r.aliasUris()) {
            aliasKeys.add(References.normalise(a));
        }
        NewExternal ne = new NewExternal(
                r.name(),
                r.namespaceUri(),
                References.normalise(r.namespaceUri()),
                r.aliasUris(),
                aliasKeys,
                r.sourceUri(),
                r.description(),
                creator,
                now,
                r.bytes());
        if (!store.insert(ne, UUID.randomUUID().toString())) {
            conflictCheck(r);
            throw ApiException.conflict("An external vocabulary with name '" + r.name() + "' is already registered.");
        }
        LOG.info("external vocabulary registered");
        return new ExternalVocabulary(
                r.name(), r.namespaceUri(), r.aliasUris(), r.sourceUri(), r.description(), creator, now);
    }

    private void conflictCheck(Registration r) {
        if (store.nameKeyExists(r.name())) {
            throw ApiException.conflict("An external vocabulary with name '" + r.name() + "' is already registered.");
        }
        Map<String, String> keyToSent = new LinkedHashMap<>();
        keyToSent.put(References.normalise(r.namespaceUri()), r.namespaceUri());
        for (String a : r.aliasUris()) {
            keyToSent.putIfAbsent(References.normalise(a), a);
        }
        Set<String> used = store.usedKeys(keyToSent.keySet());
        for (Map.Entry<String, String> e : keyToSent.entrySet()) {
            if (used.contains(e.getKey())) {
                throw ApiException.conflict(
                        "An external vocabulary with namespace or alias '" + e.getValue() + "' is already registered.");
            }
        }
    }

    /**
     * External search (D38): URIs normalised, matched against namespace and aliases, results in the
     * order of the first matching URI, each external vocabulary once.
     */
    public List<ExternalVocabulary> search(List<String> uris) {
        Set<String> keys = new LinkedHashSet<>();
        for (String u : uris) {
            keys.add(References.normalise(u));
        }
        Map<String, String> owners = store.ownersOfKeys(keys);
        Set<String> names = new LinkedHashSet<>();
        for (String k : keys) {
            String owner = owners.get(k);
            if (owner != null) {
                names.add(owner);
            }
        }
        return store.load(new ArrayList<>(names));
    }
}

package eu.simpl.vocabularymanager.content;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.Names;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Triple;
import org.springframework.stereotype.Component;

/**
 * Vocabulary references (D11, D12, D47, D69): rule S7 and the warnings externalReferenceSkipped and
 * deprecatedVocabularyReference. One store query per upload (Q5).
 */
@Component
public class ReferenceChecker {

    static final Set<String> W3C_CORE = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private static final Pattern POSITIVE = Pattern.compile("[1-9][0-9]{0,9}");
    private static final Pattern NAME_AND_TAIL = Pattern.compile("([^/#]*)(.*)");
    private static final String VERSIONS = "/versions/";

    private final String contentRoot;
    private final VocabularyRepository repository;

    public ReferenceChecker(AppProperties properties, VocabularyRepository repository) {
        this.contentRoot = properties.contentBaseTrimmed() + "/content/";
        this.repository = repository;
    }

    /** Result: S7 errors and warnings, both in declaration order. */
    public record Outcome(List<ContentIssue> errors, List<ValidationWarning> warnings) {}

    /** A parsed internal reference: version is null for the latest version. */
    record InternalReference(String name, String version) {}

    /**
     * Checks every prefix declaration of the document.
     *
     * @param ownName the vocabulary being created or versioned; references to it are ignored
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Outcome check(TurtleDocument document, String ownName) {
        List<TurtleDocument.PrefixDeclaration> prefixes = document.prefixes();
        Set<String> names = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        for (TurtleDocument.PrefixDeclaration prefix : prefixes) {
            InternalReference ref = internal(prefix.namespace());
            if (ref == null) {
                keys.add(ExternalVocabularyRepository.normalise(prefix.namespace()));
            } else if (!ref.name().equals(ownName)) {
                names.add(ref.name());
            }
        }
        if (prefixes.isEmpty()) {
            return new Outcome(List.of(), List.of());
        }
        keys.removeAll(W3C_CORE);
        ReferenceState state = repository.references(new ArrayList<>(names), new ArrayList<>(keys));
        Set<String> registered = state.externalKeySet();
        Set<String> subjects = subjectIris(document.triples());

        List<ContentIssue> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (TurtleDocument.PrefixDeclaration prefix : prefixes) {
            String location = "prefix '" + prefix.prefix() + "' -> '" + prefix.namespace() + "'";
            InternalReference ref = internal(prefix.namespace());
            if (ref == null) {
                checkExternal(prefix, location, registered, subjects, warnings);
            } else if (!ref.name().equals(ownName)) {
                checkInternal(ref, prefix.sequence(), location, state, errors, warnings);
            }
        }
        return new Outcome(errors, warnings);
    }

    /** Parses an internal reference, or null when the namespace is external (D12, D69). */
    InternalReference internal(String namespace) {
        if (!namespace.startsWith(contentRoot)) {
            return null;
        }
        Matcher m = NAME_AND_TAIL.matcher(namespace.substring(contentRoot.length()));
        if (!m.matches() || !Names.isVocabularyName(m.group(1))) {
            return null;
        }
        String name = m.group(1);
        String tail = m.group(2);
        if (tail.isEmpty() || "#".equals(tail) || "/".equals(tail)) {
            return new InternalReference(name, null);
        }
        if (tail.startsWith(VERSIONS)) {
            String version = tail.substring(VERSIONS.length());
            if (version.endsWith("#") || version.endsWith("/")) {
                version = version.substring(0, version.length() - 1);
            }
            return new InternalReference(name, version);
        }
        return null;
    }

    private static void checkExternal(
            TurtleDocument.PrefixDeclaration prefix,
            String location,
            Set<String> registered,
            Set<String> subjects,
            List<ValidationWarning> warnings) {
        String key = ExternalVocabularyRepository.normalise(prefix.namespace());
        if (W3C_CORE.contains(key) || registered.contains(key) || ownNamespace(prefix.namespace(), subjects)) {
            return;
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                "Cannot resolve external namespace '" + prefix.namespace()
                        + "' because no cached external vocabulary is registered.",
                location));
    }

    private static void checkInternal(
            InternalReference ref,
            int sequence,
            String location,
            ReferenceState state,
            List<ContentIssue> errors,
            List<ValidationWarning> warnings) {
        String name = ref.name();
        int version;
        if (ref.version() == null) {
            OptionalInt latest = state.latestVersion(name);
            if (latest.isEmpty()) {
                errors.add(new ContentIssue(
                        "Referenced internal vocabulary '" + name + "' does not exist.", location, sequence));
                return;
            }
            version = latest.getAsInt();
        } else {
            if (!POSITIVE.matcher(ref.version()).matches()
                    || Long.parseLong(ref.version()) > Integer.MAX_VALUE
                    || state.version(name, Integer.parseInt(ref.version())).isEmpty()) {
                errors.add(new ContentIssue(
                        "Referenced internal vocabulary '" + name + "' version " + ref.version() + " does not exist.",
                        location,
                        sequence));
                return;
            }
            version = Integer.parseInt(ref.version());
        }
        ReferenceState.VersionRow row = state.version(name, version).orElseThrow();
        boolean vocabularyDeprecated = row.vocabularyStatus() == VocabularyStatus.DEPRECATED;
        if (!vocabularyDeprecated && row.versionStatus() != VocabularyStatus.DEPRECATED) {
            return;
        }
        StringBuilder description = new StringBuilder("Referenced internal vocabulary '")
                .append(name)
                .append("' version ")
                .append(version)
                .append(" is deprecated.");
        if (vocabularyDeprecated) {
            description.append(" The vocabulary '").append(name).append("' is deprecated.");
        } else {
            OptionalInt active = state.latestActiveVersion(name);
            if (active.isPresent() && active.getAsInt() != version) {
                description.append(" Use version ").append(active.getAsInt()).append(", the latest ACTIVE version.");
            }
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }

    private static boolean ownNamespace(String namespace, Set<String> subjects) {
        if (namespace.isEmpty()) {
            return false;
        }
        for (String subject : subjects) {
            if (subject.startsWith(namespace)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> subjectIris(List<Triple> triples) {
        Set<String> subjects = new HashSet<>();
        for (Triple t : triples) {
            if (t.getSubject().isURI()) {
                subjects.add(t.getSubject().getURI());
            }
        }
        return subjects;
    }
}

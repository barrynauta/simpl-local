package eu.simpl.vocabulary.domain.validation;

import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vocabulary references (D11, D12, D47, D69): classifies each prefix declaration as an internal reference,
 * an external reference or an ignored one, and checks them against the store data loaded in one query (Q5).
 */
public final class ReferenceAnalyzer {

    /** W3C core namespaces that never need a cached copy (D12), normalised. */
    static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private static final Pattern INTERNAL = Pattern.compile("^([^/#]+)(?:/versions/([^/#]+))?([#/]?)$");

    /** A reference after classification. */
    public sealed interface Reference permits Internal, External {
        PrefixDeclaration declaration();
    }

    /**
     * Reference to an internal vocabulary.
     *
     * @param declaration the prefix declaration
     * @param name the vocabulary name
     * @param rawVersion the version as written, null for the latest version
     */
    public record Internal(PrefixDeclaration declaration, String name, String rawVersion) implements Reference {}

    /**
     * Reference to an external namespace that needs a registered external vocabulary.
     *
     * @param declaration the prefix declaration
     * @param key the normalised namespace
     */
    public record External(PrefixDeclaration declaration, String key) implements Reference {}

    /**
     * Store facts about one internal vocabulary.
     *
     * @param name the name
     * @param status vocabulary status
     * @param versions version status by version number
     */
    public record InternalFacts(String name, VocabularyStatus status, NavigableMap<Long, VocabularyStatus> versions) {}

    /**
     * Store facts the check needs, loaded in one query.
     *
     * @param vocabularies facts by exact name
     * @param registeredKeys normalised namespace and alias URIs of registered external vocabularies
     */
    public record ReferenceFacts(Map<String, InternalFacts> vocabularies, Set<String> registeredKeys) {}

    /**
     * Outcome of the check.
     *
     * @param errors S7 errors
     * @param warnings warnings in declaration order
     */
    public record Outcome(List<ValidationError> errors, List<ValidationWarning> warnings) {}

    private final String contentPrefix;

    public ReferenceAnalyzer(String contentBaseUrl) {
        this.contentPrefix = contentBaseUrl + "/content/";
    }

    /**
     * Classifies the declarations. References to the vocabulary itself, to W3C core namespaces and to the
     * file's own namespaces are left out.
     *
     * @param prefixes declarations in file order
     * @param ownName name of the vocabulary being created or versioned
     * @param subjectIris subject IRIs of the file
     * @return references that need a store lookup
     */
    public List<Reference> classify(List<PrefixDeclaration> prefixes, String ownName, Set<String> subjectIris) {
        List<Reference> references = new ArrayList<>();
        for (PrefixDeclaration declaration : prefixes) {
            Optional<Internal> internal = internal(declaration);
            if (internal.isPresent()) {
                if (!internal.get().name().equals(ownName)) {
                    references.add(internal.get());
                }
                continue;
            }
            String namespace = declaration.namespace();
            String key = FieldRules.normaliseNamespace(namespace);
            if (W3C_NAMESPACES.contains(key) || isOwnNamespace(namespace, subjectIris)) {
                continue;
            }
            references.add(new External(declaration, key));
        }
        return references;
    }

    private Optional<Internal> internal(PrefixDeclaration declaration) {
        String namespace = declaration.namespace();
        if (!namespace.startsWith(contentPrefix)) {
            return Optional.empty();
        }
        Matcher matcher = INTERNAL.matcher(namespace.substring(contentPrefix.length()));
        if (!matcher.matches() || !FieldRules.isValidName(matcher.group(1))) {
            return Optional.empty();
        }
        return Optional.of(new Internal(declaration, matcher.group(1), matcher.group(2)));
    }

    private static boolean isOwnNamespace(String namespace, Set<String> subjectIris) {
        if (namespace.isEmpty()) {
            return false;
        }
        for (String subject : subjectIris) {
            if (subject.startsWith(namespace)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Names of the internal vocabularies the references need.
     *
     * @param references classified references
     * @return the names
     */
    public static Set<String> internalNames(List<Reference> references) {
        Set<String> names = new LinkedHashSet<>();
        references.forEach(r -> {
            if (r instanceof Internal internal) {
                names.add(internal.name());
            }
        });
        return names;
    }

    /**
     * Checks the references against the store facts.
     *
     * @param references classified references
     * @param facts store facts
     * @return errors and warnings
     */
    public Outcome check(List<Reference> references, ReferenceFacts facts) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (Reference reference : references) {
            switch (reference) {
                case Internal internal -> checkInternal(internal, facts, errors, warnings);
                case External external -> {
                    if (!facts.registeredKeys().contains(external.key())) {
                        warnings.add(new ValidationWarning(
                                ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                                "Cannot resolve external namespace '"
                                        + external.declaration().namespace()
                                        + "' because no cached external vocabulary is registered.",
                                external.declaration().location()));
                    }
                }
            }
        }
        return new Outcome(errors, warnings);
    }

    private static void checkInternal(
            Internal reference, ReferenceFacts facts, List<ValidationError> errors, List<ValidationWarning> warnings) {
        String name = reference.name();
        String location = reference.declaration().location();
        InternalFacts vocabulary = facts.vocabularies().get(name);
        if (reference.rawVersion() == null) {
            if (vocabulary == null || vocabulary.versions().isEmpty()) {
                errors.add(
                        new ValidationError("Referenced internal vocabulary '" + name + "' does not exist.", location));
                return;
            }
            long latest = vocabulary.versions().lastKey();
            warnIfDeprecated(vocabulary, latest, location, warnings);
            return;
        }
        Optional<Long> version = FieldRules.parseVersion(reference.rawVersion());
        if (vocabulary == null || version.isEmpty() || !vocabulary.versions().containsKey(version.get())) {
            errors.add(new ValidationError(
                    "Referenced internal vocabulary '" + name + "' version " + reference.rawVersion()
                            + " does not exist.",
                    location));
            return;
        }
        warnIfDeprecated(vocabulary, version.get(), location, warnings);
    }

    /** D11 with D47: warn for an effectively deprecated version (D3). */
    private static void warnIfDeprecated(
            InternalFacts vocabulary, long version, String location, List<ValidationWarning> warnings) {
        boolean vocabularyDeprecated = vocabulary.status() == VocabularyStatus.DEPRECATED;
        boolean versionDeprecated = vocabulary.versions().get(version) == VocabularyStatus.DEPRECATED;
        if (!vocabularyDeprecated && !versionDeprecated) {
            return;
        }
        StringBuilder description = new StringBuilder("Referenced internal vocabulary '")
                .append(vocabulary.name())
                .append("' version ")
                .append(version)
                .append(" is deprecated.");
        if (vocabularyDeprecated) {
            description.append(" The vocabulary '").append(vocabulary.name()).append("' is deprecated.");
        } else {
            latestActive(vocabulary.versions())
                    .filter(active -> active != version)
                    .ifPresent(active ->
                            description.append(" Use version ").append(active).append(", the latest ACTIVE version."));
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }

    /**
     * The latest ACTIVE version: the highest-numbered version with version status ACTIVE (D5).
     *
     * @param versions version status by number
     * @return the version number, if any
     */
    public static Optional<Long> latestActive(NavigableMap<Long, VocabularyStatus> versions) {
        for (Map.Entry<Long, VocabularyStatus> entry : versions.descendingMap().entrySet()) {
            if (entry.getValue() == VocabularyStatus.ACTIVE) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
}

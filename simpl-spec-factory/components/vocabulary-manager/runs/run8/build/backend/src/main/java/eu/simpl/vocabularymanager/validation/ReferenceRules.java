package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.graph.Triple;

/**
 * Vocabulary references (D12, D47, D69): rule S7 and the warnings deprecatedVocabularyReference (D11) and
 * externalReferenceSkipped. Needs at most one store query per upload (Q5).
 */
public final class ReferenceRules {

    private static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");
    private static final String VERSIONS_SEGMENT = "/versions/";

    private ReferenceRules() {}

    /** A reference to an internal vocabulary, latest when version is null. */
    record InternalReference(PrefixDeclaration declaration, String name, String version) {}

    /**
     * @param content the parsed content
     * @param vocabularyName the vocabulary being created or versioned; references to it are ignored
     * @param contentBase contentBase of E2
     * @param lookup the store port
     * @return S7 errors and reference warnings, in prefix declaration order
     */
    public static ValidationOutcome check(
            ParsedContent content, String vocabularyName, String contentBase, ReferenceLookup lookup) {
        String internalRoot = contentBase + "/content/";
        Set<String> subjects = subjectIris(content.triples());
        List<Object> references = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        Set<Long> versions = new LinkedHashSet<>();
        Set<String> externalKeys = new LinkedHashSet<>();
        for (PrefixDeclaration declaration : content.prefixes()) {
            Optional<InternalReference> internal = internalReference(declaration, internalRoot);
            if (internal.isPresent()) {
                if (!internal.get().name().equals(vocabularyName)) {
                    references.add(internal.get());
                    names.add(internal.get().name());
                    if (internal.get().version() != null && FieldRules.isValidVersion(internal.get().version())) {
                        versions.add(Long.parseLong(internal.get().version()));
                    }
                }
            } else if (needsExternalLookup(declaration.namespace(), subjects)) {
                references.add(declaration);
                externalKeys.add(FieldRules.normalizeNamespace(declaration.namespace()));
            }
        }
        if (references.isEmpty()) {
            return new ValidationOutcome(List.of(), List.of());
        }
        ReferenceLookup.State state = lookup.lookup(names, versions, externalKeys);
        List<ContentIssue> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (Object reference : references) {
            if (reference instanceof InternalReference internal) {
                checkInternal(internal, state, errors, warnings);
            } else if (reference instanceof PrefixDeclaration declaration) {
                checkExternal(declaration, state, warnings);
            }
        }
        return new ValidationOutcome(errors, warnings);
    }

    /**
     * @param declaration a prefix declaration
     * @param internalRoot contentBase + '/content/'
     * @return the internal reference, empty when the namespace is external (D12, D69)
     */
    static Optional<InternalReference> internalReference(PrefixDeclaration declaration, String internalRoot) {
        String namespace = declaration.namespace();
        if (!namespace.startsWith(internalRoot)) {
            return Optional.empty();
        }
        String rest = namespace.substring(internalRoot.length());
        int end = endOfSegment(rest, 0);
        String name = rest.substring(0, end);
        if (!FieldRules.isValidName(name)) {
            return Optional.empty();
        }
        String after = rest.substring(end);
        if (after.startsWith(VERSIONS_SEGMENT)) {
            int versionEnd = endOfSegment(after, VERSIONS_SEGMENT.length());
            String version = after.substring(VERSIONS_SEGMENT.length(), versionEnd);
            return Optional.of(new InternalReference(declaration, name, version));
        }
        return Optional.of(new InternalReference(declaration, name, null));
    }

    private static int endOfSegment(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '/' || c == '#') {
                return i;
            }
        }
        return text.length();
    }

    private static boolean needsExternalLookup(String namespace, Set<String> subjects) {
        if (W3C_NAMESPACES.contains(FieldRules.normalizeNamespace(namespace))) {
            return false;
        }
        for (String subject : subjects) {
            if (subject.startsWith(namespace)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> subjectIris(List<Triple> triples) {
        Set<String> subjects = new HashSet<>();
        for (Triple triple : triples) {
            if (triple.getSubject().isURI()) {
                subjects.add(triple.getSubject().getURI());
            }
        }
        return subjects;
    }

    private static void checkInternal(
            InternalReference reference,
            ReferenceLookup.State state,
            List<ContentIssue> errors,
            List<ValidationWarning> warnings) {
        String location = reference.declaration().location();
        ReferenceLookup.InternalVocabularyState vocabulary =
                state.internal().get(reference.name());
        if (reference.version() == null) {
            if (vocabulary == null) {
                errors.add(new ContentIssue(
                        "Referenced internal vocabulary '" + reference.name() + "' does not exist.", location));
                return;
            }
            warnIfDeprecated(vocabulary, vocabulary.latestVersion(), location, warnings);
            return;
        }
        String missing = "Referenced internal vocabulary '" + reference.name() + "' version " + reference.version()
                + " does not exist.";
        if (vocabulary == null || !FieldRules.isValidVersion(reference.version())) {
            errors.add(new ContentIssue(missing, location));
            return;
        }
        long version = Long.parseLong(reference.version());
        if (version > vocabulary.latestVersion() || !vocabulary.versionStatuses().containsKey(version)) {
            errors.add(new ContentIssue(missing, location));
            return;
        }
        warnIfDeprecated(vocabulary, version, location, warnings);
    }

    private static void warnIfDeprecated(
            ReferenceLookup.InternalVocabularyState vocabulary,
            long version,
            String location,
            List<ValidationWarning> warnings) {
        VocabularyStatus versionStatus = vocabulary.versionStatuses().get(version);
        boolean vocabularyDeprecated = vocabulary.status() == VocabularyStatus.DEPRECATED;
        if (!vocabularyDeprecated && versionStatus != VocabularyStatus.DEPRECATED) {
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
            Optional<Long> latestActive = vocabulary.latestActiveVersion();
            if (latestActive.isPresent() && latestActive.get() != version) {
                description.append(" Use version ").append(latestActive.get()).append(", the latest ACTIVE version.");
            }
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }

    private static void checkExternal(
            PrefixDeclaration declaration, ReferenceLookup.State state, List<ValidationWarning> warnings) {
        String key = FieldRules.normalizeNamespace(declaration.namespace());
        if (state.registeredExternalKeys().contains(key)) {
            return;
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                "Cannot resolve external namespace '" + declaration.namespace()
                        + "' because no cached external vocabulary is registered.",
                declaration.location()));
    }
}

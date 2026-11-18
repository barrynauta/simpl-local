package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.springframework.stereotype.Component;

/**
 * Checks vocabulary references (D11, D12, D47): rule S7 (missing internal vocabulary or version) and the warnings
 * deprecatedVocabularyReference and externalReferenceSkipped.
 */
@Component
public class ReferenceChecker {

    private static final Pattern INTERNAL_REST = Pattern.compile("^([^/#]+)(?:/versions/([0-9]{1,9}))?(?:[#/].*)?$");
    private static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private final String internalPrefix;

    public ReferenceChecker(VocabularyProperties properties) {
        this.internalPrefix = properties.contentBase() + "/content/";
    }

    /**
     * Checks each prefix declaration.
     *
     * @param content parsed content
     * @param context lookups and the name of the vocabulary itself
     * @param errors receives S7 errors
     * @param warnings receives warnings
     */
    public void check(
            ParsedContent content,
            ReferenceContext context,
            List<ValidationError> errors,
            List<ValidationWarning> warnings) {
        for (PrefixDeclaration declaration : content.prefixes()) {
            String namespace = declaration.namespace();
            Matcher internal = namespace.startsWith(internalPrefix)
                    ? INTERNAL_REST.matcher(namespace.substring(internalPrefix.length()))
                    : null;
            if (internal != null && internal.matches()) {
                checkInternal(declaration, internal, context, errors, warnings);
            } else if (!isKnownExternal(namespace, content.graph(), context.externalKeys())) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + namespace
                                + "' because no cached external vocabulary is registered.",
                        declaration.location()));
            }
        }
    }

    private static void checkInternal(
            PrefixDeclaration declaration,
            Matcher internal,
            ReferenceContext context,
            List<ValidationError> errors,
            List<ValidationWarning> warnings) {
        String name = internal.group(1);
        Integer requested = internal.group(2) == null ? null : Integer.valueOf(internal.group(2));
        if (name.equals(context.selfName())) {
            return;
        }
        Optional<InternalVocabularyState> state = context.internalLookup().apply(name);
        if (state.isEmpty()) {
            errors.add(new ValidationError(missingText(name, requested), declaration.location()));
            return;
        }
        int number = requested == null ? state.get().vocabulary().latestVersion() : requested;
        Optional<VocabularyVersion> version = state.get().version(number);
        if (version.isEmpty()) {
            errors.add(new ValidationError(missingText(name, number), declaration.location()));
            return;
        }
        Status vocabularyStatus = state.get().vocabulary().status();
        if (version.get().status() == Status.DEPRECATED || vocabularyStatus == Status.DEPRECATED) {
            warnings.add(new ValidationWarning(
                    ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE,
                    deprecatedText(name, number, state.get()),
                    declaration.location()));
        }
    }

    static String missingText(String name, Integer version) {
        return version == null
                ? "Referenced internal vocabulary '" + name + "' does not exist."
                : "Referenced internal vocabulary '" + name + "' version " + version + " does not exist.";
    }

    static String deprecatedText(String name, int version, InternalVocabularyState state) {
        String text = "Referenced internal vocabulary '" + name + "' version " + version + " is deprecated.";
        if (state.vocabulary().status() == Status.DEPRECATED) {
            return text + " The vocabulary '" + name + "' is deprecated.";
        }
        Optional<Integer> active = state.latestActiveVersion();
        if (active.isPresent() && active.get() != version) {
            return text + " Use version " + active.get() + ", the latest ACTIVE version.";
        }
        return text;
    }

    private static boolean isKnownExternal(String namespace, Graph graph, Set<String> externalKeys) {
        String normalised = ExternalVocabulary.normalise(namespace);
        if (externalKeys.contains(normalised) || W3C_NAMESPACES.contains(normalised)) {
            return true;
        }
        return isOwnNamespace(namespace, graph);
    }

    private static boolean isOwnNamespace(String namespace, Graph graph) {
        if (namespace.isEmpty()) {
            return false;
        }
        return graph.find()
                .filterKeep(t -> {
                    Node s = t.getSubject();
                    return s.isURI() && s.getURI().startsWith(namespace);
                })
                .hasNext();
    }
}

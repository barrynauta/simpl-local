package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.Namespaces;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.WarningCode;
import eu.simpl.vocabularymanager.error.Issue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.springframework.stereotype.Component;

/**
 * Checks the vocabulary references of uploaded content (decisions D11, D12): rule S7 (missing internal vocabulary or
 * version) and the warnings deprecatedVocabularyReference and externalReferenceSkipped.
 */
@Component
public class ReferenceRules {

    private final Pattern internalPattern;

    public ReferenceRules(VocabularyManagerProperties properties) {
        this.internalPattern = Pattern.compile("^" + Pattern.quote(properties.contentBaseUrl())
                + "/content/([A-Za-z0-9]+)(?:/versions/([0-9]+))?[#/]?$");
    }

    /** Result of the reference check. */
    public record Result(List<Issue> errors, List<ValidationWarning> warnings) {}

    /**
     * Checks every prefix declaration.
     *
     * @param prefixes the references in file order
     * @param graph the parsed content, to find the vocabulary's own namespace
     * @param ownName the vocabulary being created or versioned; references to it are ignored
     * @param lookup read access to stored vocabularies
     */
    public Result check(List<PrefixDeclaration> prefixes, Graph graph, String ownName, ReferenceLookup lookup) {
        List<Issue> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Optional<InternalVocabularyState>> cache = new HashMap<>();
        Set<String> subjects = subjectIris(graph);
        for (PrefixDeclaration declaration : prefixes) {
            Matcher matcher = internalPattern.matcher(declaration.namespace());
            if (matcher.matches()) {
                checkInternal(declaration, matcher, ownName, lookup, cache, errors, warnings);
            } else if (isUnresolvedExternal(declaration.namespace(), subjects, lookup)) {
                warnings.add(new ValidationWarning(
                        WarningCode.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + declaration.namespace()
                                + "' because no cached external vocabulary is registered.",
                        declaration.location()));
            }
        }
        return new Result(errors, warnings);
    }

    private static void checkInternal(
            PrefixDeclaration declaration,
            Matcher matcher,
            String ownName,
            ReferenceLookup lookup,
            Map<String, Optional<InternalVocabularyState>> cache,
            List<Issue> errors,
            List<ValidationWarning> warnings) {
        String name = matcher.group(1);
        if (ownName != null && name.toLowerCase(Locale.ROOT).equals(ownName.toLowerCase(Locale.ROOT))) {
            return;
        }
        String versionText = matcher.group(2);
        Optional<InternalVocabularyState> state = cache.computeIfAbsent(name, lookup::internalVocabulary);
        Integer requested = parseVersion(versionText);
        if (state.isEmpty()
                || (versionText != null && !state.get().versionStatuses().containsKey(requested))) {
            String detail = versionText == null
                    ? "Referenced internal vocabulary '" + name + "' does not exist."
                    : "Referenced internal vocabulary '" + name + "' version " + versionText + " does not exist.";
            errors.add(Issue.invalidInput(TurtleSyntaxValidator.FIELD, detail, declaration.location()));
            return;
        }
        InternalVocabularyState vocabulary = state.get();
        int version = requested == null ? vocabulary.latestVersion() : requested;
        if (vocabulary.isEffectivelyDeprecated(version)) {
            warnings.add(new ValidationWarning(
                    WarningCode.DEPRECATED_VOCABULARY_REFERENCE,
                    deprecatedDescription(name, version, vocabulary.latestActiveVersion()),
                    declaration.location()));
        }
    }

    static String deprecatedDescription(String name, int version, OptionalInt latestActive) {
        String text = "Referenced internal vocabulary '" + name + "' version " + version + " is deprecated.";
        if (latestActive.isPresent()) {
            text += " Use version " + latestActive.getAsInt() + ", the latest ACTIVE version.";
        }
        return text;
    }

    private static Integer parseVersion(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isUnresolvedExternal(String namespace, Set<String> subjects, ReferenceLookup lookup) {
        String normalised = Namespaces.normalise(namespace);
        if (Namespaces.W3C.contains(normalised)) {
            return false;
        }
        if (!namespace.isEmpty() && subjects.stream().anyMatch(subject -> subject.startsWith(namespace))) {
            return false;
        }
        return !lookup.isRegisteredExternal(namespace);
    }

    private static Set<String> subjectIris(Graph graph) {
        return graph.find(Node.ANY, Node.ANY, Node.ANY)
                .mapWith(triple -> triple.getSubject())
                .filterKeep(Node::isURI)
                .mapWith(Node::getURI)
                .toSet();
    }
}

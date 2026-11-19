package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.TurtleParser.PrefixDeclaration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

/**
 * Checks the vocabulary references of uploaded content (decisions D11, D12, D47): internal references must exist
 * (rule S7) and give a warning when effectively deprecated; external references give a warning unless they are
 * registered, a W3C core namespace or the vocabulary's own namespace.
 */
public class ReferenceChecker {

    private static final Pattern INTERNAL_PATH =
            Pattern.compile("^(?<name>[^/#]+)(?:/versions/(?<version>[^/#]+))?(?:[#/].*)?$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,9}$");
    private static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private final VocabularyStore store;
    private final String internalPrefix;

    /**
     * Result of the reference check.
     *
     * @param errors S7 findings
     * @param warnings validation warnings
     */
    public record Result(List<SemanticRules.Finding> errors, List<ValidationWarning> warnings) {}

    /**
     * Creates the checker.
     *
     * @param store the store, read for referenced vocabularies and registered external namespaces
     * @param contentBaseUrl the content base (decision E2)
     */
    public ReferenceChecker(VocabularyStore store, String contentBaseUrl) {
        this.store = store;
        String base = contentBaseUrl.endsWith("/")
                ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1)
                : contentBaseUrl;
        this.internalPrefix = base + "/content/";
    }

    /**
     * Checks the references.
     *
     * @param prefixes the prefix declarations in order
     * @param graph the parsed content, for the own-namespace test
     * @param ownName the vocabulary being created or versioned; references to it are ignored
     * @return errors and warnings
     */
    public Result check(List<PrefixDeclaration> prefixes, Graph graph, String ownName) {
        List<SemanticRules.Finding> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        List<PrefixDeclaration> external = new ArrayList<>();
        for (PrefixDeclaration declaration : prefixes) {
            Optional<Matcher> internal = internalMatch(declaration.namespace());
            if (internal.isPresent()) {
                checkInternal(declaration, internal.get(), ownName, errors, warnings);
            } else {
                external.add(declaration);
            }
        }
        warnings.addAll(checkExternal(external, graph));
        return new Result(errors, warnings);
    }

    private Optional<Matcher> internalMatch(String namespace) {
        if (!namespace.startsWith(internalPrefix)) {
            return Optional.empty();
        }
        Matcher matcher = INTERNAL_PATH.matcher(namespace.substring(internalPrefix.length()));
        return matcher.matches() ? Optional.of(matcher) : Optional.empty();
    }

    private void checkInternal(
            PrefixDeclaration declaration,
            Matcher matcher,
            String ownName,
            List<SemanticRules.Finding> errors,
            List<ValidationWarning> warnings) {
        String name = matcher.group("name");
        String versionText = matcher.group("version");
        if (name.equals(ownName)) {
            return;
        }
        String location = location(declaration);
        Optional<Vocabulary> vocabulary = store.findVocabulary(name);
        if (vocabulary.isEmpty()) {
            errors.add(new SemanticRules.Finding(missingText(name, versionText), location));
            return;
        }
        Optional<VocabularyVersion> version = versionText == null
                ? store.findVersion(name, vocabulary.get().latestVersion())
                : DIGITS.matcher(versionText).matches()
                        ? store.findVersion(name, Long.parseLong(versionText))
                        : Optional.empty();
        if (version.isEmpty()) {
            errors.add(new SemanticRules.Finding(missingText(name, versionText), location));
            return;
        }
        boolean vocabularyDeprecated = vocabulary.get().status() == VocabularyStatus.DEPRECATED;
        if (vocabularyDeprecated || version.get().status() == VocabularyStatus.DEPRECATED) {
            warnings.add(new ValidationWarning(
                    ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE,
                    deprecatedText(name, version.get().version(), vocabularyDeprecated),
                    location));
        }
    }

    private String deprecatedText(String name, int version, boolean vocabularyDeprecated) {
        String text = "Referenced internal vocabulary '" + name + "' version " + version + " is deprecated.";
        if (vocabularyDeprecated) {
            return text + " The vocabulary '" + name + "' is deprecated.";
        }
        Optional<Integer> latestActive = store.latestActiveVersion(name);
        if (latestActive.isPresent() && latestActive.get() != version) {
            return text + " Use version " + latestActive.get() + ", the latest ACTIVE version.";
        }
        return text;
    }

    private static String missingText(String name, String versionText) {
        if (versionText == null) {
            return "Referenced internal vocabulary '" + name + "' does not exist.";
        }
        return "Referenced internal vocabulary '" + name + "' version " + versionText + " does not exist.";
    }

    private List<ValidationWarning> checkExternal(List<PrefixDeclaration> declarations, Graph graph) {
        List<PrefixDeclaration> candidates = new ArrayList<>();
        Set<String> subjects = null;
        for (PrefixDeclaration declaration : declarations) {
            String key = ExternalVocabulary.normalise(declaration.namespace());
            if (W3C_NAMESPACES.contains(key)) {
                continue;
            }
            if (subjects == null) {
                subjects = subjectIris(graph);
            }
            if (isOwnNamespace(declaration.namespace(), subjects)) {
                continue;
            }
            candidates.add(declaration);
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        candidates.forEach(d -> keys.add(ExternalVocabulary.normalise(d.namespace())));
        Set<String> registered = new HashSet<>();
        for (ExternalVocabulary external : store.findExternalsByUris(List.copyOf(keys))) {
            registered.add(ExternalVocabulary.normalise(external.namespaceUri()));
            external.aliasUris().forEach(alias -> registered.add(ExternalVocabulary.normalise(alias)));
        }
        List<ValidationWarning> warnings = new ArrayList<>();
        for (PrefixDeclaration declaration : candidates) {
            if (!registered.contains(ExternalVocabulary.normalise(declaration.namespace()))) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + declaration.namespace()
                                + "' because no cached external vocabulary is registered.",
                        location(declaration)));
            }
        }
        return warnings;
    }

    private static Set<String> subjectIris(Graph graph) {
        Set<String> subjects = new HashSet<>();
        graph.find(Node.ANY, Node.ANY, Node.ANY).forEachRemaining(t -> {
            if (t.getSubject().isURI()) {
                subjects.add(t.getSubject().getURI());
            }
        });
        return subjects;
    }

    private static boolean isOwnNamespace(String namespace, Set<String> subjects) {
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

    private static String location(PrefixDeclaration declaration) {
        return "prefix '" + declaration.prefix() + "' -> '" + declaration.namespace() + "'";
    }
}

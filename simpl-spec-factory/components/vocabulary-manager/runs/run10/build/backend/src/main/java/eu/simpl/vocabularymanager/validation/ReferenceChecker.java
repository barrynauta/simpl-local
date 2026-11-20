package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.Issue;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

/**
 * The reference check of D11, D12, D47 and D69: internal references to missing vocabularies or versions are
 * validation errors (S7); references to effectively deprecated versions and unregistered external namespaces are
 * warnings. It needs at most one store query per upload (Q5), through {@link ReferenceLookup}.
 */
public class ReferenceChecker {

    static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]{2,63}$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    /** Stored state the check needs, read in one store query. */
    public interface ReferenceLookup {
        ReferenceData lookup(Set<String> internalNames, Set<String> externalKeys);
    }

    /** One referenced internal vocabulary: its status, latest version and the status of each version. */
    public record InternalState(VocabularyStatus status, int latestVersion, Map<Integer, VocabularyStatus> versions) {}

    /** The result of the lookup: internal vocabularies by name and the registered external lookup keys. */
    public record ReferenceData(Map<String, InternalState> internal, Set<String> registeredExternalKeys) {}

    /** Errors (S7) and warnings found. */
    public record Result(List<Issue> errors, List<ValidationWarning> warnings) {}

    private final String contentPrefix;

    public ReferenceChecker(String contentBase) {
        this.contentPrefix = contentBase + "/content/";
    }

    public Result check(ParsedContent content, String ownName, ReferenceLookup lookup) {
        List<Reference> references = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        List<String> subjects = subjectIris(content.graph());
        for (ParsedContent.PrefixDeclaration declaration : content.prefixes()) {
            Reference reference = classify(declaration);
            if (reference.internalName() != null) {
                if (reference.internalName().equals(ownName)) {
                    continue;
                }
                names.add(reference.internalName());
            } else {
                String key = ExternalVocabulary.key(declaration.namespace());
                if (W3C_NAMESPACES.contains(key) || isOwnNamespace(declaration.namespace(), subjects)) {
                    continue;
                }
                keys.add(key);
            }
            references.add(reference);
        }
        if (references.isEmpty()) {
            return new Result(List.of(), List.of());
        }
        ReferenceData data = lookup.lookup(names, keys);
        List<Issue> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (Reference reference : references) {
            if (reference.internalName() != null) {
                checkInternal(reference, data, errors, warnings);
            } else if (!data.registeredExternalKeys().contains(ExternalVocabulary.key(reference.namespace()))) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + reference.namespace()
                                + "' because no cached external vocabulary is registered.",
                        reference.declaration().location()));
            }
        }
        return new Result(errors, warnings);
    }

    private static void checkInternal(
            Reference reference, ReferenceData data, List<Issue> errors, List<ValidationWarning> warnings) {
        String name = reference.internalName();
        String location = reference.declaration().location();
        String versionText = reference.versionText();
        if (versionText != null && !isPositiveInt(versionText)) {
            errors.add(missing(name, versionText, location));
            return;
        }
        InternalState state = data.internal().get(name);
        if (state == null) {
            errors.add(missing(name, versionText, location));
            return;
        }
        int version = versionText == null ? state.latestVersion() : Integer.parseInt(versionText);
        VocabularyStatus versionStatus = state.versions().get(version);
        if (versionStatus == null) {
            errors.add(missing(name, Integer.toString(version), location));
            return;
        }
        boolean vocabularyDeprecated = state.status() == VocabularyStatus.DEPRECATED;
        if (versionStatus != VocabularyStatus.DEPRECATED && !vocabularyDeprecated) {
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
            int latestActive = state.versions().entrySet().stream()
                    .filter(entry -> entry.getValue() == VocabularyStatus.ACTIVE && entry.getKey() != version)
                    .mapToInt(Map.Entry::getKey)
                    .max()
                    .orElse(-1);
            if (latestActive > 0) {
                description.append(" Use version ").append(latestActive).append(", the latest ACTIVE version.");
            }
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }

    private static Issue missing(String name, String versionText, String location) {
        String detail = versionText == null
                ? "Referenced internal vocabulary '" + name + "' does not exist."
                : "Referenced internal vocabulary '" + name + "' version " + versionText + " does not exist.";
        return Issue.invalidInput("vocabularyFile", detail, location);
    }

    private static boolean isPositiveInt(String text) {
        if (!DIGITS.matcher(text).matches() || text.length() > 10) {
            return false;
        }
        long value = Long.parseLong(text);
        return value >= 1 && value <= Integer.MAX_VALUE;
    }

    /** A namespace that is the prefix of at least one subject IRI of the file is the vocabulary's own (D12). */
    private static boolean isOwnNamespace(String namespace, List<String> subjects) {
        return subjects.stream().anyMatch(subject -> subject.startsWith(namespace));
    }

    private static List<String> subjectIris(Graph graph) {
        Set<String> subjects = new LinkedHashSet<>();
        graph.find().forEachRemaining(triple -> {
            Node subject = triple.getSubject();
            if (subject.isURI()) {
                subjects.add(subject.getURI());
            }
        });
        return new ArrayList<>(subjects);
    }

    /** Splits a namespace into an internal reference (name and optional version text) or an external one. */
    Reference classify(ParsedContent.PrefixDeclaration declaration) {
        String namespace = declaration.namespace();
        if (!namespace.startsWith(contentPrefix)) {
            return new Reference(declaration, namespace, null, null);
        }
        String rest = namespace.substring(contentPrefix.length());
        int end = indexOfAny(rest, 0);
        String name = rest.substring(0, end);
        if (!NAME.matcher(name).matches()) {
            return new Reference(declaration, namespace, null, null);
        }
        String after = rest.substring(end);
        if (after.startsWith("/versions/")) {
            String tail = after.substring("/versions/".length());
            String versionText = tail.substring(0, indexOfAny(tail, 0));
            return new Reference(declaration, namespace, name, versionText);
        }
        return new Reference(declaration, namespace, name, null);
    }

    private static int indexOfAny(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '/' || c == '#') {
                return i;
            }
        }
        return text.length();
    }

    /** One vocabulary reference; internalName is null for an external reference. */
    record Reference(
            ParsedContent.PrefixDeclaration declaration, String namespace, String internalName, String versionText) {}
}

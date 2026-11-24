package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validation of uploaded content (D9 to D12, D47, D65, D125): syntax first, then the semantic rules S1 to S7 with
 * all errors in one report, then the warnings of the reference check. The reference check uses one store query
 * per upload (Q5).
 */
@Component
public class ContentValidator {

    public static final String SYNTAX_PREFIX = "Encountered invalid Turtle syntax";

    private final String contentBase;
    private final VocabularyRepository repository;

    public ContentValidator(AppProperties properties, VocabularyRepository repository) {
        String base = properties.contentBaseUrl();
        this.contentBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.repository = repository;
    }

    /** contentBase without trailing slash. */
    public String contentBase() {
        return contentBase;
    }

    /**
     * Validates internal vocabulary content.
     *
     * @param content the uploaded bytes
     * @param name the vocabulary being created or versioned
     * @param version the version number it will get (base IRI, D57)
     * @return the validation warnings
     * @throws ApiException 400 with the validation report when there are errors
     */
    public List<ValidationWarning> validateInternal(byte[] content, String name, long version) {
        ParsedContent parsed = parseOrReject(content, contentBase + "/content/" + name + "/versions/" + version);
        List<SemanticRules.PositionedIssue> issues = new ArrayList<>(SemanticRules.check(parsed));
        References.Found references = References.find(parsed, contentBase, name);
        List<ValidationWarning> warnings = checkReferences(references, issues);
        if (!issues.isEmpty()) {
            issues.sort(Comparator.comparingInt(SemanticRules.PositionedIssue::position));
            throw ApiException.badRequest(
                    issues.stream().map(SemanticRules.PositionedIssue::issue).toList());
        }
        return warnings;
    }

    /**
     * Validates external vocabulary content: syntax only and not empty (D30, D70, D124).
     *
     * @param content the uploaded bytes
     * @param namespaceUri the base for relative IRIs
     * @throws ApiException 400 with one issue
     */
    public void validateExternal(byte[] content, String namespaceUri) {
        ParsedContent parsed = parseOrReject(content, namespaceUri);
        if (parsed.triples().isEmpty()) {
            throw ApiException.badRequest(
                    List.of(Issue.invalidInput("body", "vocabularyFile", SemanticRules.S6, "document")));
        }
    }

    private static ParsedContent parseOrReject(byte[] content, String base) {
        try {
            return TurtleSyntax.parse(content, base);
        } catch (TurtleSyntax.SyntaxError e) {
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "" : ": " + e.getMessage();
            String detail = SYNTAX_PREFIX + sanitise(message);
            throw new ApiException(
                    ProblemType.BAD_REQUEST,
                    null,
                    List.of(Issue.invalidInput("body", "vocabularyFile", detail, e.location())));
        }
    }

    private List<ValidationWarning> checkReferences(
            References.Found references, List<SemanticRules.PositionedIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        Set<Long> versions = new HashSet<>();
        for (References.Internal reference : references.internal()) {
            names.add(reference.name());
            if (reference.version() != null) {
                versions.add(reference.version());
            }
        }
        Set<String> keys = new LinkedHashSet<>();
        for (ParsedContent.PrefixDeclaration declaration : references.external()) {
            keys.add(References.normalise(declaration.resolved()));
        }
        VocabularyRepository.Lookup lookup = repository.lookup(names, versions, keys);
        Map<String, Known> known = new HashMap<>();
        for (VocabularyRepository.VersionRow row : lookup.rows()) {
            Known entry = known.computeIfAbsent(
                    row.name(), key -> new Known(row.vocabularyStatus(), row.latestVersion(), new HashMap<>()));
            if (row.version() > 0) {
                entry.versions().put(row.version(), row.versionStatus());
            }
        }
        List<ValidationWarning> warnings = new ArrayList<>();
        for (References.Internal reference : references.internal()) {
            internalOutcome(reference, known.get(reference.name()), issues, warnings);
        }
        Set<String> registered = new HashSet<>(lookup.registeredKeys());
        for (ParsedContent.PrefixDeclaration declaration : references.external()) {
            if (!registered.contains(References.normalise(declaration.resolved()))) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + declaration.declared()
                                + "' because no cached external vocabulary is registered.",
                        location(declaration)));
            }
        }
        return warnings;
    }

    private record Known(VocabularyStatus status, long latest, Map<Long, VocabularyStatus> versions) {}

    private static void internalOutcome(
            References.Internal reference,
            Known known,
            List<SemanticRules.PositionedIssue> issues,
            List<ValidationWarning> warnings) {
        String name = reference.name();
        String location = location(reference.declaration());
        int position = reference.declaration().position();
        if (known == null || (reference.versionText() != null && reference.version() == null)) {
            String detail = reference.versionText() == null
                    ? "Referenced internal vocabulary '" + name + "' does not exist."
                    : "Referenced internal vocabulary '" + name + "' version " + reference.versionText()
                            + " does not exist.";
            issues.add(new SemanticRules.PositionedIssue(position, missing(detail, location)));
            return;
        }
        long version = reference.version() == null ? known.latest() : reference.version();
        VocabularyStatus versionStatus = known.versions().get(version);
        if (versionStatus == null) {
            String detail = "Referenced internal vocabulary '" + name + "' version " + reference.versionText()
                    + " does not exist.";
            issues.add(new SemanticRules.PositionedIssue(position, missing(detail, location)));
            return;
        }
        boolean vocabularyDeprecated = known.status() == VocabularyStatus.DEPRECATED;
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
            long latestActive = known.versions().entrySet().stream()
                    .filter(entry -> entry.getValue() == VocabularyStatus.ACTIVE)
                    .mapToLong(Map.Entry::getKey)
                    .max()
                    .orElse(-1);
            if (latestActive > 0 && latestActive != version) {
                description.append(" Use version ").append(latestActive).append(", the latest ACTIVE version.");
            }
        }
        warnings.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }

    private static Issue missing(String detail, String location) {
        return Issue.invalidInput("body", "vocabularyFile", detail, location);
    }

    private static String location(ParsedContent.PrefixDeclaration declaration) {
        return "prefix '" + declaration.prefix() + "' -> '" + declaration.declared() + "'";
    }

    private static String sanitise(String message) {
        String text = message.replaceAll("[\\r\\n\\t]+", " ");
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}

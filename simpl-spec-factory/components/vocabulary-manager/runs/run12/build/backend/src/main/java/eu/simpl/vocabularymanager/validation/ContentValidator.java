package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.Model.Warning;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.store.ReferenceStore;
import eu.simpl.vocabularymanager.store.ReferenceStore.InternalInfo;
import eu.simpl.vocabularymanager.store.ReferenceStore.Lookup;
import eu.simpl.vocabularymanager.validation.References.Ref;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.springframework.stereotype.Component;

/**
 * The two validation steps of D9 for internal vocabularies (syntax, then semantic S1 to S7 with the
 * warnings of D10 to D12, D47) and the syntax-only check of external registrations (D30, D70,
 * D124). The reference check uses one store query per upload (Q5).
 */
@Component
public class ContentValidator {

    public static final int MAX_ISSUES = 1000;
    public static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";
    public static final String EXTERNAL_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_REFERENCE = "deprecatedVocabularyReference";

    private final ReferenceStore references;
    private final String contentBase;

    public ContentValidator(ReferenceStore references, AppProperties props) {
        this.references = references;
        this.contentBase = props.contentBaseUrl();
    }

    /**
     * Validates the content of a new version of {@code name}.
     *
     * @return the validation warnings (D10)
     * @throws ApiException 400 with the validation report when there is an error
     */
    public List<Warning> validateInternal(byte[] bytes, String name, int version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        ParsedTurtle parsed = parseOrFail(bytes, base);
        List<SemanticRules.Finding> errors = new ArrayList<>(SemanticRules.check(parsed));
        List<KeyedWarning> warnings = new ArrayList<>();
        checkReferences(parsed, name, errors, warnings);
        if (!errors.isEmpty()) {
            errors.sort(Comparator.comparingInt(SemanticRules.Finding::position));
            boolean truncated = errors.size() > MAX_ISSUES;
            List<Issue> issues = new ArrayList<>();
            for (SemanticRules.Finding f : errors.subList(0, Math.min(MAX_ISSUES, errors.size()))) {
                issues.add(Issue.invalidInput("body", "vocabularyFile", f.detail(), f.location()));
            }
            throw new ApiException(ProblemType.BAD_REQUEST, ApiException.BAD_INPUT, issues, Map.of(), truncated);
        }
        warnings.sort(Comparator.comparingInt(KeyedWarning::position));
        List<Warning> out = new ArrayList<>();
        for (KeyedWarning w : warnings) {
            out.add(w.warning());
        }
        return out;
    }

    /** Syntax check of an external vocabulary file; the namespace URI is the base (D124). */
    public void validateExternal(byte[] bytes, String namespaceUri) {
        ParsedTurtle parsed = parseOrFail(bytes, namespaceUri);
        if (parsed.triples().isEmpty()) {
            throw ApiException.badRequest(Issue.invalidInput("body", "vocabularyFile", SemanticRules.S6, "document"));
        }
    }

    private static ParsedTurtle parseOrFail(byte[] bytes, String base) {
        try {
            return ParsedTurtle.parse(bytes, base);
        } catch (ParsedTurtle.SyntaxError e) {
            throw ApiException.badRequest(
                    Issue.invalidInput("body", "vocabularyFile", SYNTAX_DETAIL + ": " + e.getMessage(), e.location()));
        }
    }

    private record KeyedWarning(Warning warning, int position) {}

    private void checkReferences(
            ParsedTurtle parsed, String ownName, List<SemanticRules.Finding> errors, List<KeyedWarning> warnings) {
        List<Ref> refs = References.classify(parsed.prefixes(), contentBase);
        Set<String> names = new LinkedHashSet<>();
        Set<Integer> versions = new LinkedHashSet<>();
        Set<String> externalKeys = new LinkedHashSet<>();
        List<Ref> internal = new ArrayList<>();
        List<Ref> external = new ArrayList<>();
        for (Ref r : refs) {
            if (r.internal()) {
                if (r.internalName().equals(ownName)) {
                    continue;
                }
                internal.add(r);
                names.add(r.internalName());
                if (r.versionText() != null && r.versionNumber() > 0) {
                    versions.add(r.versionNumber());
                }
            } else {
                String key = References.normalise(r.namespace());
                if (References.W3C.contains(key) || isOwnNamespace(parsed, r.namespace())) {
                    continue;
                }
                external.add(r);
                externalKeys.add(key);
            }
        }
        if (internal.isEmpty() && external.isEmpty()) {
            return;
        }
        Lookup lookup = references.lookup(names, versions, true, externalKeys);
        for (Ref r : internal) {
            InternalInfo info = lookup.internal().get(r.internalName());
            String n = r.internalName();
            if (r.versionText() != null) {
                int v = r.versionNumber();
                Status vs = info == null || v < 1 ? null : info.versions().get(v);
                if (vs == null) {
                    errors.add(new SemanticRules.Finding(
                            "Referenced internal vocabulary '"
                                    + n
                                    + "' version "
                                    + r.versionText()
                                    + " does not exist.",
                            r.location(),
                            r.position()));
                    continue;
                }
                deprecatedWarning(r, info, v, vs, warnings);
            } else {
                if (info == null) {
                    errors.add(new SemanticRules.Finding(
                            "Referenced internal vocabulary '" + n + "' does not exist.", r.location(), r.position()));
                    continue;
                }
                Status vs = info.versions().get(info.latest());
                deprecatedWarning(r, info, info.latest(), vs, warnings);
            }
        }
        for (Ref r : external) {
            if (!lookup.externalKeys().contains(References.normalise(r.namespace()))) {
                warnings.add(new KeyedWarning(
                        new Warning(
                                EXTERNAL_SKIPPED,
                                "Cannot resolve external namespace '"
                                        + r.namespace()
                                        + "' because no cached external vocabulary is registered.",
                                r.location()),
                        r.position()));
            }
        }
    }

    /** The deprecated-reference warning of D11 and D47 for an effectively deprecated version. */
    private static void deprecatedWarning(
            Ref r, InternalInfo info, int version, Status versionStatus, List<KeyedWarning> warnings) {
        boolean vocabularyDeprecated = info.status() == Status.DEPRECATED;
        if (!vocabularyDeprecated && versionStatus != Status.DEPRECATED) {
            return;
        }
        String n = r.internalName();
        StringBuilder d = new StringBuilder("Referenced internal vocabulary '")
                .append(n)
                .append("' version ")
                .append(version)
                .append(" is deprecated.");
        if (vocabularyDeprecated) {
            d.append(" The vocabulary '").append(n).append("' is deprecated.");
        } else {
            int m = info.latestActive();
            if (m > 0 && m != version) {
                d.append(" Use version ").append(m).append(", the latest ACTIVE version.");
            }
        }
        warnings.add(new KeyedWarning(new Warning(DEPRECATED_REFERENCE, d.toString(), r.location()), r.position()));
    }

    /** A namespace that is the start of at least one subject IRI in the file (D12). */
    static boolean isOwnNamespace(ParsedTurtle parsed, String namespace) {
        if (namespace.isEmpty()) {
            return false;
        }
        for (Triple t : parsed.triples()) {
            Node s = t.getSubject();
            if (s.isURI() && s.getURI().startsWith(namespace)) {
                return true;
            }
        }
        return false;
    }
}

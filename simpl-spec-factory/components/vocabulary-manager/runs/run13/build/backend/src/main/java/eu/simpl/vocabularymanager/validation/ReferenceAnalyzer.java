package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.UriKeys;
import eu.simpl.vocabularymanager.validation.ParsedContent.PrefixDeclaration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.jena.graph.Triple;

/**
 * Finds the vocabulary references of an upload (D12, D69, D145, D146): each distinct namespace declared
 * with a prefix, internal when it lies under the content URL of a vocabulary, external otherwise.
 */
public final class ReferenceAnalyzer {

    static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]{2,63}$");
    private static final Pattern POSITIVE_INTEGER = Pattern.compile("^[0-9]+$");
    private static final Set<String> W3C_KEYS = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    /** A reference found in the content. */
    public sealed interface Reference permits InternalReference, ExternalReference {
        PrefixDeclaration declaration();

        default String location() {
            return "prefix '" + declaration().prefix() + "' -> '"
                    + declaration().namespace() + "'";
        }
    }

    /**
     * A reference to a vocabulary of this component.
     *
     * @param versionText the version segment as written, or null for a reference to the latest version
     */
    public record InternalReference(PrefixDeclaration declaration, String name, String versionText)
            implements Reference {

        /** The version number, or null when absent, not a positive integer or out of range. */
        public Integer version() {
            if (versionText == null || !POSITIVE_INTEGER.matcher(versionText).matches()) {
                return null;
            }
            try {
                int value = Integer.parseInt(versionText);
                return value >= 1 ? value : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /** A reference to a namespace that is not a vocabulary of this component. */
    public record ExternalReference(PrefixDeclaration declaration) implements Reference {
        public String key() {
            return UriKeys.key(declaration.namespace());
        }
    }

    private final String contentPrefix;

    /** @param contentBase the content base URL of E2, without a trailing slash */
    public ReferenceAnalyzer(String contentBase) {
        this.contentPrefix = contentBase + "/content/";
    }

    /**
     * The references to check. References to the vocabulary itself, W3C core namespaces and the file's own
     * namespaces are left out.
     *
     * @param ownName name of the vocabulary being created or versioned
     */
    public List<Reference> references(ParsedContent content, String ownName) {
        Set<String> subjects = subjectIris(content.triples());
        Set<String> seen = new HashSet<>();
        List<Reference> references = new ArrayList<>();
        for (PrefixDeclaration declaration : content.prefixes()) {
            if (!seen.add(declaration.namespace())) {
                continue;
            }
            InternalReference internal = internal(declaration);
            if (internal != null) {
                if (!internal.name().equals(ownName)) {
                    references.add(internal);
                }
                continue;
            }
            ExternalReference external = new ExternalReference(declaration);
            if (!W3C_KEYS.contains(external.key()) && !ownNamespace(declaration.namespace(), subjects)) {
                references.add(external);
            }
        }
        return references;
    }

    private InternalReference internal(PrefixDeclaration declaration) {
        String namespace = declaration.namespace();
        if (!namespace.startsWith(contentPrefix)) {
            return null;
        }
        String rest = namespace.substring(contentPrefix.length());
        int end = indexOfAny(rest, 0);
        String name = rest.substring(0, end);
        if (!NAME.matcher(name).matches()) {
            return null;
        }
        String after = rest.substring(end);
        String versionsPrefix = "/versions/";
        if (after.startsWith(versionsPrefix)) {
            int versionEnd = indexOfAny(after, versionsPrefix.length());
            String versionText = after.substring(versionsPrefix.length(), versionEnd);
            if (!versionText.isEmpty()) {
                return new InternalReference(declaration, name, versionText);
            }
        }
        return new InternalReference(declaration, name, null);
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

    private static Set<String> subjectIris(List<Triple> triples) {
        Set<String> subjects = new LinkedHashSet<>();
        for (Triple triple : triples) {
            if (triple.getSubject().isURI()) {
                subjects.add(triple.getSubject().getURI());
            }
        }
        return subjects;
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
}

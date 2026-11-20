package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.validation.ReferenceAnalysis.ExternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalysis.InternalReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Classifies prefix declarations into internal and external references (D12, D69). No store access. */
public final class ReferenceAnalyzer {

    /** W3C namespaces that never need a registered copy (D12). */
    static final Set<String> W3C_KEYS = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private static final Pattern NAME = Pattern.compile("[A-Z][a-zA-Z0-9]{2,63}");
    private static final String VERSIONS = "/versions/";

    private final String contentPrefix;

    /** @param contentBase the configured content base URL without trailing slash (E2) */
    public ReferenceAnalyzer(String contentBase) {
        this.contentPrefix = contentBase + "/content/";
    }

    /**
     * Classifies the declarations of one upload.
     *
     * @param prefixes declarations in document order
     * @param subjectIris subject IRIs of the upload (own namespace rule)
     * @param ownName name of the vocabulary being created or versioned; its references are ignored
     * @return the classified references
     */
    public ReferenceAnalysis analyze(List<PrefixDeclaration> prefixes, Set<String> subjectIris, String ownName) {
        List<InternalReference> internal = new ArrayList<>();
        List<ExternalReference> external = new ArrayList<>();
        for (PrefixDeclaration declaration : prefixes) {
            InternalReference reference = internalReference(declaration);
            if (reference != null) {
                if (!reference.name().equals(ownName)) {
                    internal.add(reference);
                }
                continue;
            }
            String key = UriKeys.key(declaration.namespace());
            if (W3C_KEYS.contains(key) || isOwnNamespace(declaration.namespace(), subjectIris)) {
                continue;
            }
            external.add(new ExternalReference(declaration, key));
        }
        return new ReferenceAnalysis(List.copyOf(internal), List.copyOf(external));
    }

    private InternalReference internalReference(PrefixDeclaration declaration) {
        String namespace = declaration.namespace();
        if (!namespace.startsWith(contentPrefix)) {
            return null;
        }
        String rest = namespace.substring(contentPrefix.length());
        int end = boundary(rest, 0);
        String name = rest.substring(0, end);
        if (!NAME.matcher(name).matches()) {
            return null;
        }
        String tail = rest.substring(end);
        if (!tail.startsWith(VERSIONS)) {
            return new InternalReference(declaration, name, null);
        }
        String afterVersions = tail.substring(VERSIONS.length());
        String version = afterVersions.substring(0, boundary(afterVersions, 0));
        return new InternalReference(declaration, name, version);
    }

    private static int boundary(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '/' || c == '#') {
                return i;
            }
        }
        return text.length();
    }

    private static boolean isOwnNamespace(String namespace, Set<String> subjectIris) {
        for (String subject : subjectIris) {
            if (subject.startsWith(namespace)) {
                return true;
            }
        }
        return false;
    }
}

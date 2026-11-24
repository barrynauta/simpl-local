package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.VocabularyNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.jena.graph.Triple;

/**
 * Finds the vocabulary references of uploaded content (D12, D69, D145, D146, D171, D192, D198): each distinct
 * declared namespace is internal, external or ignored.
 */
public final class References {

    /** W3C core namespaces of D12, compared without a trailing '#' or '/' (D198). */
    public static final Set<String> W3C_NAMESPACES = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private static final Pattern POSITIVE_INTEGER = Pattern.compile("^[1-9][0-9]{0,17}$");

    private References() {}

    /**
     * An internal reference.
     *
     * @param declaration the prefix declaration
     * @param name the referenced vocabulary name
     * @param versionText the version as written, or null for the latest version
     * @param version the version number, or null when there is none or it is not a positive integer
     */
    public record Internal(
            ParsedContent.PrefixDeclaration declaration, String name, String versionText, Long version) {}

    /**
     * The references of one upload.
     *
     * @param internal internal references in file order
     * @param external external namespaces to look up, in file order
     */
    public record Found(List<Internal> internal, List<ParsedContent.PrefixDeclaration> external) {}

    /**
     * Classifies the prefix declarations.
     *
     * @param content the parsed content
     * @param contentBase contentBase of E2, without trailing slash
     * @param ownName the vocabulary being created or versioned; references to it are ignored
     * @return the references
     */
    public static Found find(ParsedContent content, String contentBase, String ownName) {
        String internalPrefix = contentBase + "/content/";
        Set<String> seen = new HashSet<>();
        Set<String> subjects = subjectIris(content.triples());
        List<Internal> internal = new ArrayList<>();
        List<ParsedContent.PrefixDeclaration> external = new ArrayList<>();
        for (ParsedContent.PrefixDeclaration declaration : content.prefixes()) {
            String namespace = declaration.resolved();
            if (!seen.add(namespace)) {
                continue;
            }
            Internal reference = internalReference(declaration, internalPrefix);
            if (reference != null) {
                if (!reference.name().equals(ownName)) {
                    internal.add(reference);
                }
                continue;
            }
            if (W3C_NAMESPACES.contains(normalise(namespace)) || isOwnNamespace(namespace, subjects)) {
                continue;
            }
            external.add(declaration);
        }
        return new Found(internal, external);
    }

    /** Removes one trailing '#' or '/' for comparisons (D30, D73). */
    public static String normalise(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private static Internal internalReference(ParsedContent.PrefixDeclaration declaration, String internalPrefix) {
        String namespace = declaration.resolved();
        if (!namespace.startsWith(internalPrefix)) {
            return null;
        }
        String rest = namespace.substring(internalPrefix.length());
        int end = 0;
        while (end < rest.length() && Character.isLetterOrDigit(rest.charAt(end)) && rest.charAt(end) < 128) {
            end++;
        }
        String name = rest.substring(0, end);
        String after = rest.substring(end);
        if (!VocabularyNames.isValid(name)) {
            return null;
        }
        if (!after.isEmpty() && !after.startsWith("#") && !after.startsWith("/")) {
            return null;
        }
        if (!after.startsWith("/versions/")) {
            return new Internal(declaration, name, null, null);
        }
        String tail = after.substring("/versions/".length());
        int stop = 0;
        while (stop < tail.length() && tail.charAt(stop) != '#' && tail.charAt(stop) != '/') {
            stop++;
        }
        String versionText = tail.substring(0, stop);
        Long version = POSITIVE_INTEGER.matcher(versionText).matches() ? Long.valueOf(versionText) : null;
        return new Internal(declaration, name, versionText, version);
    }

    private static boolean isOwnNamespace(String namespace, Set<String> subjects) {
        for (String subject : subjects) {
            if (subject.startsWith(namespace)) {
                return true;
            }
        }
        return false;
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
}

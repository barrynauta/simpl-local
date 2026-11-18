package eu.simpl.vocabularymanager.domain;

import java.util.Set;

/** Namespace helpers shared by validation and external vocabulary lookups (decisions D12, D30). */
public final class Namespaces {

    /** The W3C namespaces that never need a cached external copy (decision D12), normalised. */
    public static final Set<String> W3C = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    private Namespaces() {}

    /** Removes one trailing '#' or '/'. */
    public static String normalise(String uri) {
        if (uri == null) {
            return "";
        }
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}

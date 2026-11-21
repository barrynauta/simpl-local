package eu.simpl.vocabularymanager.domain;

/** Comparison key of a namespace or alias URI: a trailing '#' or '/' removed (D30, D73). */
public final class UriKeys {

    private UriKeys() {}

    public static String key(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}

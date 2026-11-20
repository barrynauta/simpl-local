package eu.simpl.vocabularymanager.validation;

import org.apache.jena.rfc3986.RFC3986;

/** URI form of D73: an RFC 3986 URI with a scheme, parsed by the Jena RFC 3986 parser. */
public final class UriRules {

    public static final int MAX_LENGTH = 2048;

    private UriRules() {}

    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_LENGTH) {
            return false;
        }
        try {
            return RFC3986.create(value).hasScheme();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}

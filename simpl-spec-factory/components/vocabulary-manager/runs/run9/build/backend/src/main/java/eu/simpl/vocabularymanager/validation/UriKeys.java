package eu.simpl.vocabularymanager.validation;

import java.net.URI;
import java.net.URISyntaxException;

/** URI helpers of D30 and D73. */
public final class UriKeys {

    private UriKeys() {}

    /** Comparison key: the URI without one trailing '#' or '/' (D30). */
    public static String key(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /** Whether the value is an absolute URI with a scheme (RFC 3986, D73). */
    public static boolean isAbsolute(String value) {
        if (value == null || value.isEmpty() || value.length() > 2048) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x20 || c >= 0x7f || c == '<' || c == '>' || c == '"' || c == '\\' || c == '^' || c == '`'
                    || c == '{' || c == '|' || c == '}') {
                return false;
            }
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() && uri.getScheme() != null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}

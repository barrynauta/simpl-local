package eu.simpl.vocabularymanager.api;

import java.net.URI;
import java.net.URISyntaxException;

/** Checks of format: uri values (RFC 3986 absolute URI, parsed with java.net.URI). */
final class UriValues {

    private static final int MAX_LENGTH = 2048;

    private UriValues() {}

    static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_LENGTH) {
            return false;
        }
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}

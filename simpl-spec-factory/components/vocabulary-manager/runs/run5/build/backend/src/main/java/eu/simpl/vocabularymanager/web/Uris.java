package eu.simpl.vocabularymanager.web;

import java.net.URI;
import java.net.URISyntaxException;

/** Checks of 'format: uri' values. */
public final class Uris {

    private Uris() {}

    /** True when the text is an absolute URI. */
    public static boolean isAbsoluteUri(String text) {
        if (text == null || text.isBlank() || text.length() > 2048) {
            return false;
        }
        try {
            URI uri = new URI(text);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}

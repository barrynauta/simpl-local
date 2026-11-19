package eu.simpl.vocabularymanager.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Field rules of D37, D56 and D73, shared by the API and the reference check. */
public final class FieldRules {

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 64;
    public static final int TEXT_MAX = 1000;
    public static final int URI_MAX = 2048;
    public static final int MAX_ALIASES = 20;
    private static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern VERSION = Pattern.compile("^[1-9][0-9]{0,17}$");

    private FieldRules() {}

    /**
     * @param name a vocabulary or external vocabulary name
     * @return true when it meets D37: 3 to 64 characters, ^[A-Z][a-zA-Z0-9]*$
     */
    public static boolean isValidName(String name) {
        return name != null
                && name.length() >= NAME_MIN
                && name.length() <= NAME_MAX
                && NAME.matcher(name).matches();
    }

    /**
     * @param name a name
     * @return the case-insensitive uniqueness key (D37)
     */
    public static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * @param value a path or reference version value
     * @return true when it is a positive integer (D49)
     */
    public static boolean isValidVersion(String value) {
        return value != null && VERSION.matcher(value).matches();
    }

    /**
     * @param text a description or changelog, not yet trimmed
     * @return true when it has 1 to 1000 characters after trimming (D37)
     */
    public static boolean isValidRequiredText(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        return length >= 1 && length <= TEXT_MAX;
    }

    /**
     * @param text an optional description, not yet trimmed
     * @return true when it has at most 1000 characters after trimming (D56)
     */
    public static boolean isValidOptionalText(String text) {
        if (text == null) {
            return true;
        }
        String trimmed = text.strip();
        return trimmed.codePointCount(0, trimmed.length()) <= TEXT_MAX;
    }

    /**
     * @param value a URI as sent
     * @return true when it is an absolute URI with a scheme (RFC 3986, D73)
     */
    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > URI_MAX) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= ' ' || c == '<' || c == '>' || c == '"' || c == '{' || c == '}' || c == '|' || c == '\\'
                    || c == '^' || c == '`' || c > '~') {
                return false;
            }
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() && uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Comparison form of a namespace URI: one trailing '#' or '/' removed (D30).
     *
     * @param uri a URI as sent
     * @return the lookup key
     */
    public static String normalizeNamespace(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
